package net.tonbot.plugin.ifplayer;

import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zmpp.base.Memory;
import org.zmpp.zcode.CapabilityFlag;
import org.zmpp.zcode.InputStream;
import org.zmpp.zcode.Machine;
import org.zmpp.zcode.OutputStream;
import org.zmpp.zcode.ScreenModel;
import org.zmpp.zcode.ScreenModelWindow;
import org.zmpp.zcode.ZMachineRunStates;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import net.tonbot.plugin.ifplayer.ScreenState.ScreenStateBuilder;
import scala.Tuple2;
import scala.collection.JavaConversions;

/**
 * A state machine.
 */
class GameMachine implements ScreenModel, OutputStream {
	private static final Logger LOG = LoggerFactory.getLogger(GameMachine.class);

	/**
	 * Since Discord only allows text input, we need to map them from a string to
	 * these keycodes.
	 */
	private static final Map<String, Integer> CHAR_MAPPING = new ImmutableMap.Builder<String, Integer>()
			.put("<enter>", 13).put("<return>", 13).put("<esc>", 27).put("<up>", 129).put("<down>", 130)
			.put("<left>", 131).put("<right>", 132).put("<f1>", 133).put("<f2>", 134).put("<f3>", 135).put("<f4>", 136)
			.put("<f5>", 137).put("<f6>", 138).put("<f7>", 139).put("<f8>", 140).put("<f9>", 141).put("<f10>", 142)
			.put("<f11>", 143).put("<f12>", 144).put("<num0>", 145).put("<num1>", 146).put("<num2>", 147)
			.put("<num3>", 148).put("<num4>", 149).put("<num5>", 150).put("<num6>", 151).put("<num7>", 152)
			.put("<num8>", 153).put("<num9>", 154).build();

	private static final int UPPER_WINDOW_WIDTH = 100;

	private static final int MAX_EXPECTED_WINDOWS = 2;
	private static final int LOWER_WINDOW_INDEX = 0;
	private static final int UPPER_WINDOW_INDEX = 1;

	private static final scala.collection.immutable.List<CapabilityFlag> CAPABILITIES = JavaConversions
			.asScalaBuffer(ImmutableList.<CapabilityFlag>of()).toList();

	private final Machine vm;

	@Getter
	private final Story story;
	private final long channelId;

	private SaveFile saveFile = null;
	private OnSavedCallback fileSavedCallback;
	private boolean isSavingToFile = false;

	private List<CharacterMatrix> windows;
	private int activeWindow;
	private boolean selected = true; // No idea wtf this does.
	private boolean started = false;
	private boolean statusLineIsReadable = false;
	private boolean manuallyStopped = false;

	public GameMachine(final Story story, final long channelId, final OnSavedCallback fileSavedCallback) {
		this.story = Preconditions.checkNotNull(story, "story must be non-null.");
		this.channelId = channelId;
		this.fileSavedCallback = Preconditions.checkNotNull(fileSavedCallback, "fileSavedCallback must be non-null.");

		if (story.getVersion() == 6) {
			throw new IllegalArgumentException("Z-Machine V6 files are not supported.");
		}

		this.windows = new ArrayList<>(MAX_EXPECTED_WINDOWS);

		// The lower window should be unconstrained.
		this.windows.add(LOWER_WINDOW_INDEX, new DiscordAwareCharacterMatrix());

		// The upper window should have a constant width.
		// The height, however, can change at any time.
		this.windows.add(UPPER_WINDOW_INDEX, new DiscordAwareCharacterMatrix(UPPER_WINDOW_WIDTH, 0));

		this.activeWindow = 0;

		this.vm = new Machine();
		Memory mem = story.getMemory();
		vm.init(mem, this);

		initUI();
	}

	/**
	 * Sets the save file.
	 * 
	 * @param saveFile
	 *            The save file.
	 */
	public void setSaveFile(SaveFile saveFile) {
		Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");

		this.saveFile = saveFile;
	}

	/**
	 * Gets the save file.
	 * 
	 * @return {@link SaveFile}
	 */
	public Optional<SaveFile> getSaveFile() {
		return Optional.ofNullable(this.saveFile);
	}

	/**
	 * Gets the next screen state after providing the given input.
	 * 
	 * @param input
	 *            The input. Only nullable on the first call. Non-null on subsequent
	 *            calls.
	 * @param username
	 *            The user who sent the input.
	 * @return An optional {@link ScreenState}. Empty if the game not running or is
	 *         no longer running.
	 * @throws GameMachineException
	 *             If an error occurred with the turn. These exceptions do not
	 *             necessarily mean that the game machine has been stopped. This
	 *             exception can be thrown if the GameMachine is actually stopped,
	 *             though.
	 * 
	 */
	public Optional<ScreenState> takeTurn(String input, String username) {
		Preconditions.checkNotNull(username, "username must be non-null.");

		if (this.manuallyStopped || vm.state().runState() == ZMachineRunStates.Halted()) {
			throw new GameMachineException("This GameMachine has been stopped.");
		}

		if (this.started) {
			Preconditions.checkNotNull(!StringUtils.isEmpty(input), "input must be non-null and non-empty.");

			if (vm.state().runState() == ZMachineRunStates.ReadLine()) {
				this.readLine(input);
			} else if (vm.state().runState() == ZMachineRunStates.ReadChar()) {
				this.readChar(input);
			}
		}

		this.started = true;

		ScreenState nextScreenState = null;

		while (true) {
			while (vm.state().runState() == ZMachineRunStates.Running()) {
				vm.doInstruction(false);
			}

			if (isSavingToFile) {
				// Saving finished
				SaveFileMetadata md = SaveFileMetadata.builder().createdBy(username).creationDate(ZonedDateTime.now())
						.build();
				this.saveFile = fileSavedCallback.getOnSavedCallback(channelId, saveFile, md);
				isSavingToFile = false;
			}

			if (vm.state().runState() == ZMachineRunStates.ReadLine()
					|| vm.state().runState() == ZMachineRunStates.ReadChar()) {
				ScreenStateBuilder builder = ScreenState.builder();

				if (statusLineIsReadable) {
					builder = builder.statusLineObjectName(vm.statusLineObjectName())
							.statusLineScoreOrTime(vm.statusLineScoreOrTime());
				}

				CharacterMatrix upperWindow = windows.get(UPPER_WINDOW_INDEX);
				CharacterMatrix lowerWindow = windows.get(LOWER_WINDOW_INDEX);

				nextScreenState = builder.windowContents(ImmutableList.of(upperWindow.render(), lowerWindow.render()))
						.build();

				upperWindow.reset();
				lowerWindow.reset();

				break;
			} else if (vm.state().runState() == ZMachineRunStates.SaveGame()) {
				this.requestSaveFile();
			} else if (vm.state().runState() == ZMachineRunStates.RestoreGame()) {
				this.requestRestoreFile();
			} else if (vm.state().runState() == ZMachineRunStates.Halted()) {
				this.flush();
				LOG.info("Machine has halted.");
				break;
			}
		}

		return Optional.ofNullable(nextScreenState);
	}

	/**
	 * Checks whether if the machine is stopped.
	 * 
	 * @return True iff the machine is stopped.
	 */
	public boolean isStopped() {
		return vm.state().runState() == ZMachineRunStates.Halted() || manuallyStopped;
	}

	/**
	 * Marks the GameMachine as stopped.
	 */
	public void stop() {
		this.manuallyStopped = true;
	}

	@Override
	public ScreenModelWindow activeWindow() {
		LOG.debug("activeWindow called.");
		return null;
	}

	@Override
	public void bufferMode(int flag) {
		LOG.debug("bufferMode called with flag {}", flag);
	}

	@Override
	public void cancelInput() {
		LOG.debug("cancelInput called");
	}

	@Override
	public scala.collection.immutable.List<CapabilityFlag> capabilities() {
		return CAPABILITIES;
	}

	@Override
	public void connect(Machine arg0) {
		// GameMachine already knows the Machine instance.
		throw new UnsupportedOperationException("connect is not supported.");
	}

	@Override
	public Tuple2<Object, Object> cursorPosition() {
		if (this.activeWindow == LOWER_WINDOW_INDEX) {
			throw new IllegalStateException("Cursor is not supported on the lower window.");
		}

		CharacterMatrix charMatrix = this.windows.get(this.activeWindow);
		int[] cursorPosition = charMatrix.getCursorPosition();
		return new Tuple2<>(cursorPosition[0] + 1, cursorPosition[1] + 1);
	}

	@Override
	public void eraseLine(int value) {
		LOG.debug("eraseLine called with value {}", value);
	}

	@Override
	public void eraseWindow(int windowId) {
		LOG.debug("eraseWindow called with windowId {}", windowId);

		if (windowId == -1) {
			CharacterMatrix topWindow = this.windows.get(UPPER_WINDOW_INDEX);
			topWindow.setMaxHeight(0);
			topWindow.reset();

			CharacterMatrix bottomWindow = this.windows.get(LOWER_WINDOW_INDEX);
			bottomWindow.reset();
		} else if (windowId == -2) {
			CharacterMatrix topWindow = this.windows.get(UPPER_WINDOW_INDEX);
			topWindow.reset();

			CharacterMatrix bottomWindow = this.windows.get(LOWER_WINDOW_INDEX);
			bottomWindow.reset();
		} else if ((windowId == UPPER_WINDOW_INDEX || windowId == 3) && activeWindow == UPPER_WINDOW_INDEX) {
			this.windows.get(UPPER_WINDOW_INDEX).reset();
		} else if ((windowId == LOWER_WINDOW_INDEX || windowId == 3) && activeWindow == LOWER_WINDOW_INDEX) {
			this.windows.get(LOWER_WINDOW_INDEX).reset();
		}
	}

	@Override
	public void flushInterruptOutput() {
		LOG.debug("flushInterruptOutput called");
	}

	@Override
	public void initUI() {
		vm.setFontSizeInUnits(1, 1);

		CharacterMatrix upperWindow = this.windows.get(UPPER_WINDOW_INDEX);
		vm.setScreenSizeInUnits(upperWindow.getMaxWidth(), upperWindow.getMaxHeight());
	}

	@Override
	public InputStream keyboardStream() {
		LOG.debug("keyboardStream called.");
		return null;
	}

	@Override
	public OutputStream screenOutputStream() {
		return this;
	}

	@Override
	public void setColour(int foreground, int background, int window) {
		LOG.debug("setColour called with foreground {}, background {}, window {}", foreground, background, window);
	}

	@Override
	public void setCursorPosition(int line, int column) {
		// When the upper window is selected, its cursor position can be moved with
		// set_cursor.
		// This position is given in characters in the form (row, column), with (1,1) at
		// the top left.
		// The opcode has no effect when the lower window is selected. It is illegal to
		// move the cursor
		// outside the current size of the upper window.
		// http://inform-fiction.org/zmachine/standards/z1point1/sect08.html

		LOG.debug("setCursorPosition called with line {}, column {}", line, column);
		if (this.activeWindow == LOWER_WINDOW_INDEX) {
			LOG.debug("setCursorPosition called with line {}, column {} on the lower window, which is not permitted",
					line, column);
			return;
		}

		CharacterMatrix charMatrix = this.windows.get(this.activeWindow);
		charMatrix.setCursor(line - 1, column - 1);
	}

	@Override
	public int setFont(int font) {
		LOG.debug("setFont called with font {}", font);
		return 0;
	}

	@Override
	public void setTextStyle(int style) {
		LOG.debug("setTextStyle called with style {}", style);
	}

	@Override
	public void setWindow(int windowId) {
		// Selects the given window for text output.
		LOG.debug("setWindow called with windowId {}", windowId);
		Preconditions.checkArgument(windowId < MAX_EXPECTED_WINDOWS, "Unexpected windowId received.");
		this.activeWindow = windowId;
	}

	@Override
	public void splitWindow(int lines) {
		// Splits the screen so that the upper window has the given number of lines: or,
		// if this is zero,
		// unsplits the screen again. In Version 3 (only) the upper window should be
		// cleared after the split.
		// More at: http://inform-fiction.org/zmachine/standards/z1point1/sect15.html
		LOG.debug("splitWindow called with lines {}", lines);

		if (story.getVersion() == 3) {
			// Clears the top window.
			this.windows.get(UPPER_WINDOW_INDEX).reset();
		}

		this.windows.get(UPPER_WINDOW_INDEX).setMaxHeight(lines);
	}

	@Override
	public void updateStatusLine() {
		statusLineIsReadable = true;
	}

	private void readLine(String suppliedLine) {
		if (vm.version() <= 3) {
			this.updateStatusLine();
		}

		int maxChars = vm.readLineInfo().maxInputChars();

		// Trim the supplied input to maxChars-1 chars because we are going to add a
		// newline immediately after.
		String trimmedInput = suppliedLine.substring(0, Math.min(suppliedLine.length(), maxChars - 1));
		vm.resumeWithLineInput(trimmedInput + "\n");
	}

	@Override
	public void flush() {
		LOG.debug("flush called.");
	}

	private void readChar(String suppliedLine) {

		Integer specialChar = CHAR_MAPPING.get(suppliedLine);
		if (specialChar != null) {
			vm.resumeWithCharInput(specialChar);
		} else {
			// The supplyInput method guarantees that the supplied input is not empty.
			char character = suppliedLine.charAt(0);

			// TODO: AFAIK, the VM can't handle multibyte characters. Better check for that.
			vm.resumeWithCharInput((int) character);
		}
	}

	private void requestRestoreFile() {
		java.io.InputStream saveFileInputStream;

		try {
			if (saveFile != null) {
				saveFileInputStream = saveFile.getInputStream();
				LOG.debug("Attempting to load file at {}", saveFile.getURI());
			} else {
				saveFileInputStream = null;
			}
		} catch (UncheckedIOException e) {
			saveFileInputStream = null;
			LOG.error("Save file at {} could not be read.", saveFile.getURI(), e);
			throw new GameMachineException("Failed to load the game.", e);
		}

		vm.resumeWithRestoreStream(saveFileInputStream);
	}

	private void requestSaveFile() {
		if (saveFile == null) {
			throw new GameMachineException("Cannot save the game because there is no save file selected.");
		}

		try {
			java.io.OutputStream saveFileOutputStream = saveFile.getOutputStream();
			LOG.debug("Attempting to save file at {}", saveFile.getURI());
			vm.resumeWithSaveStream(saveFileOutputStream);
			this.isSavingToFile = true;
		} catch (UncheckedIOException e) {
			LOG.error("Save file at {} could not be accessed.", saveFile.getURI(), e);
			throw new GameMachineException("Failed to save the game.", e);
		}
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public void putChar(char c) {
		CharacterMatrix charMatrix = this.windows.get(this.activeWindow);
		charMatrix.write(c);
	}

	@Override
	public void select(boolean flag) {
		LOG.debug("select called with flag {}", flag);
		this.selected = flag;
	}

}
