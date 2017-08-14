package net.tonbot.plugin.ifplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zmpp.base.Memory;
import org.zmpp.zcode.CapabilityFlag;
import org.zmpp.zcode.InputStream;
import org.zmpp.zcode.Machine;
import org.zmpp.zcode.OutputStream;
import org.zmpp.zcode.ScreenModelWindow;
import org.zmpp.zcode.SwingScreenModel;
import org.zmpp.zcode.ZMachineRunStates;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.TonbotTechnicalFault;

import lombok.Getter;
import scala.Tuple2;
import scala.collection.JavaConversions;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

class GameStateManager implements SwingScreenModel, OutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateManager.class); 
    
    /**
     * Since Discord only allows text input, we need to map them from a string to these keycodes.
     */
    private static final Map<String, Integer> CHAR_MAPPING = new ImmutableMap.Builder<String, Integer>()
            .put("<enter>", 13)
            .put("<return>", 13)
            .put("<esc>", 27)
            .put("<up>", 129)
            .put("<down>", 130)
            .put("<left>", 131)
            .put("<right>", 132)
            .put("<f1>", 133)
            .put("<f2>", 134)
            .put("<f3>", 135)
            .put("<f4>", 136)
            .put("<f5>", 137)
            .put("<f6>", 138)
            .put("<f7>", 139)
            .put("<f8>", 140)
            .put("<f9>", 141)
            .put("<f10>", 142)
            .put("<f11>", 143)
            .put("<f12>", 144)
            .put("<num0>", 145)
            .put("<num1>", 146)
            .put("<num2>", 147)
            .put("<num3>", 148)
            .put("<num4>", 149)
            .put("<num5>", 150)
            .put("<num6>", 151)
            .put("<num7>", 152)
            .put("<num8>", 153)
            .put("<num9>", 154)
            .build();
    
    private static final int UPPER_WINDOW_WIDTH = 100;
    
    private static final int MAX_EXPECTED_WINDOWS = 2;
    private static final int LOWER_WINDOW_INDEX = 0;
    private static final int UPPER_WINDOW_INDEX = 1;
    // Window 0 is below window 1
    private static final int[] WINDOW_ORDER = new int[] {UPPER_WINDOW_INDEX, LOWER_WINDOW_INDEX};

    private List<CharacterMatrix> windows;
    private int activeWindow;

    private final Machine vm;

    @Getter
    private final Story story;
    private final File saveFile;
    private final IChannel channel;
    private final scala.collection.immutable.List<CapabilityFlag> capabilities;

    private boolean selected = true;

    private final Object userInputBlocker = new Object();
    private String suppliedInput;

    private Thread vmExecThread = null;
    private boolean stopRequested = false;
    
    // Set to true as soon as the first status line is updated by the game.
    private boolean statusLineInitiated = false;

    public GameStateManager(Story story, File saveFile, IChannel channel) {
        this.story = Preconditions.checkNotNull(story, "story must be non-null.");
        this.saveFile = Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");
        this.channel = Preconditions.checkNotNull(channel, "channel must be non-null.");
        
        if (story.getVersion() == 6) {
                throw new IllegalArgumentException("Z-Machine V6 files are not supported.");
        }

        ArrayList<CapabilityFlag> caps = new ArrayList<>();
        this.capabilities = JavaConversions.asScalaBuffer(caps).toList();

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
     * Starts the background VM execution thread. If the thread is already created, then no-op.
     */
    public void start() {
        GameStateManager stateManager = this;

        if (vmExecThread == null) {
            vmExecThread = new Thread(() -> stateManager.executeTurn(vm, stateManager));
            vmExecThread.start();
        }
    }

    /**
     * Request to stop. Repeated requests have no effect.
     */
    public void stop() {
        stopRequested = true;
        synchronized(userInputBlocker) {
            userInputBlocker.notifyAll();
        }
    }

    /**
     * Checks whether if this game is running or not.
     * @return True if it is running. False otherwise.
     */
    public boolean isRunning() {
        if (vmExecThread != null) {
            return vmExecThread.isAlive();
        } else {
            return false;
        }
    }

    private void executeTurn(Machine vm, SwingScreenModel screenModel) {
        try {
            updateChannelTopic();
            while (!stopRequested) {
                while (vm.state().runState() == ZMachineRunStates.Running()) {
                    vm.doInstruction(false);
                }

                if (vm.state().runState() == ZMachineRunStates.ReadLine()) {
                    screenModel.readLine();
                } else if (vm.state().runState() == ZMachineRunStates.ReadChar()) {
                    screenModel.readChar();
                } else if (vm.state().runState() == ZMachineRunStates.SaveGame()) {
                    screenModel.requestSaveFile();
                } else if (vm.state().runState() == ZMachineRunStates.RestoreGame()) {
                    screenModel.requestRestoreFile();
                } else if (vm.state().runState() == ZMachineRunStates.Halted()) {
                    screenModel.flush();
                    LOG.info("Machine has halted.");
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("GameStateManager has stopped in an unexpected way.", e);
            sendToChannel("The player has crashed! :(", e);
        } finally {
            updateChannelTopic();

            BotUtils.sendMessage(channel, "Story '" + this.story.getName() + "' has stopped.");
        }
    }

    @Override
    public void putChar(char c) {
        CharacterMatrix charMatrix = this.windows.get(this.activeWindow);
        charMatrix.write(c);
    }

    @Override
    public void select(boolean flag) {
        this.selected = flag;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void readChar() {
        sendWindowsToChannel();

        try {
            synchronized (userInputBlocker) {
                userInputBlocker.wait();
            }

            if (suppliedInput != null) {
                    Integer specialChar = CHAR_MAPPING.get(suppliedInput);
                    if (specialChar != null) {
                        vm.resumeWithCharInput(specialChar);
                    } else {
                        // The supplyInput method guarantees that the supplied input is not empty.
                    char character = suppliedInput.charAt(0);

                    //TODO: AFAIK, the VM can't handle multibyte characters. Better check for that.
                    vm.resumeWithCharInput((int) character);
                    }
            }
        } catch (InterruptedException e) {
            // Oh no. Should never happen.
            throw new TonbotTechnicalFault("Oh no.", e);
        }
    }

    @Override
    public void requestSaveFile() {
        java.io.OutputStream saveFileOutputStream;
        try {
            saveFile.createNewFile();
            saveFileOutputStream = new FileOutputStream(saveFile);

            LOG.debug("Attempting to save file at {}", saveFile.getAbsolutePath());
        } catch (IOException e) {
            saveFileOutputStream = null;
            LOG.error("Could not save the game at {}.", saveFile.getAbsolutePath(), e);
            sendToChannel("Could not save the game.", e);
        }

        vm.resumeWithSaveStream(saveFileOutputStream);
    }

    @Override
    public void requestRestoreFile() {
        java.io.InputStream saveFileInputStream;

        try {

            if (saveFile.exists()) {
                saveFileInputStream = new FileInputStream(saveFile);
                LOG.debug("Attempting to load file at {}", saveFile.getAbsolutePath());
                this.sendToChannel("Loading save file...");
            } else {
                saveFileInputStream = null;
                this.sendToChannel("No save file for this game.");
            }

        } catch (IOException e) {
            saveFileInputStream = null;
            LOG.error("Could not load the save file {}.", saveFile.getAbsolutePath(), e);
            this.sendToChannel("Could not load the game.", e);
        }

        vm.resumeWithRestoreStream(saveFileInputStream);
    }

    @Override
    public void flush() {
        LOG.debug("flush called.");
    }

    @Override
    public int readLine() {
        // When the machine requests line input, we assume that this is an indicator that
        // we should send out whatever is in the windows to the channel.
        sendWindowsToChannel();
        int maxChars = vm.readLineInfo().maxInputChars();

        if (vm.version() <= 3) {
            this.updateStatusLine();
        }
        
        // Now we need to block the thread until the user responds with something.
        // If we don't block, then the machine will exit.
        try {
            synchronized (userInputBlocker) {
                userInputBlocker.wait();
            }

            if (suppliedInput != null) {
                    // Trim the supplied input to maxChars-1 chars because we are going to add a newline immediately after.
                    String trimmedInput = suppliedInput.substring(0, Math.min(suppliedInput.length(), maxChars - 1));
                vm.resumeWithLineInput(trimmedInput + "\n");
            }
        } catch (InterruptedException e) {
            // Oh no. Should never happen.
            sendToChannel("Oh no.", e);
        }

        return 0;
    }

    @Override
    public OutputStream screenOutputStream() {
        return this;
    }

    @Override
    public InputStream keyboardStream() {
        LOG.debug("keyboardStream called.");
        return null;
    }

    @Override
    public ScreenModelWindow activeWindow() {
        LOG.debug("activeWindow called.");
        return null;
    }

    @Override
    public void updateStatusLine() {
            statusLineInitiated = true;
            updateChannelTopic();
    }

    @Override
    public void splitWindow(int lines) {
            // Splits the screen so that the upper window has the given number of lines: or, if this is zero,
            // unsplits the screen again. In Version 3 (only) the upper window should be cleared after the split.
            // More at: http://inform-fiction.org/zmachine/standards/z1point1/sect15.html
        LOG.debug("splitWindow called with lines {}", lines);
        
        if (story.getVersion() == 3) {
                // Clears the top window.
                this.windows.get(UPPER_WINDOW_INDEX).reset();
        }
        
        this.windows.get(UPPER_WINDOW_INDEX).setMaxHeight(lines);
    }

    @Override
    public void setWindow(int windowId) {
            // Selects the given window for text output.
        LOG.debug("setWindow called with windowId {}", windowId);
        Preconditions.checkArgument(windowId < MAX_EXPECTED_WINDOWS, "Unexpected windowId received.");
        this.activeWindow = windowId;
    }

    @Override
    public void setCursorPosition(int line, int column) {
        // When the upper window is selected, its cursor position can be moved with set_cursor. 
        // This position is given in characters in the form (row, column), with (1,1) at the top left. 
        // The opcode has no effect when the lower window is selected. It is illegal to move the cursor 
        // outside the current size of the upper window.
        // http://inform-fiction.org/zmachine/standards/z1point1/sect08.html
        
        LOG.debug("setCursorPosition called with line {}, column {}", line, column);
        if (this.activeWindow == LOWER_WINDOW_INDEX) {
            LOG.debug("setCursorPosition called with line {}, column {} on the lower window, which is not permitted", line, column);
                return;
        }
        
        CharacterMatrix charMatrix = this.windows.get(this.activeWindow);
        charMatrix.setCursor(line - 1, column - 1);
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
    public void connect(Machine vm) {
        throw new UnsupportedOperationException("Not to be used.");
    }

    @Override
    public void initUI() {
        LOG.debug("initUI called");
        
        vm.setFontSizeInUnits(1, 1);
        
        CharacterMatrix upperWindow = this.windows.get(UPPER_WINDOW_INDEX);
        vm.setScreenSizeInUnits(upperWindow.getMaxWidth(), upperWindow.getMaxHeight());
    }

    @Override
    public void flushInterruptOutput() {
        LOG.debug("flushInterruptOutput called");
    }

    @Override
    public void cancelInput() {
        LOG.debug("cancelInput called");
    }

    @Override
    public void bufferMode(int flag) {
        LOG.debug("bufferMode called with flag {}", flag);
    }

    @Override
    public void eraseWindow(int windowId) {
        LOG.debug("eraseWindow called with windowId {}", windowId);
    }

    @Override
    public void eraseLine(int value) {
        LOG.debug("eraseLine called with value {}", value);
    }

    @Override
    public void setTextStyle(int style) {
        LOG.debug("setTextStyle called with style {}", style);
    }

    @Override
    public int setFont(int font) {
        LOG.debug("setFont called with font {}", font);
        return 0;
    }

    @Override
    public void setColour(int foreground, int background, int window) {
        LOG.debug("setColour called with foreground {}, background {}, window {}", foreground, background, window);

    }

    @Override
    public scala.collection.immutable.List<CapabilityFlag> capabilities() {
        return capabilities;
    }

    /**
     * Supplies an input to the state machine. Empty inputs will be ignored.
     * @param input Input. Non-null.
     */
    public void supplyInput(String input) {
            Preconditions.checkNotNull(input, "input must be non-null.");
            
            if (input.isEmpty()) {
                return;
            }
            
        this.suppliedInput = input;
        synchronized (userInputBlocker) {
            userInputBlocker.notify();
        }
    }

    private void sendWindowsToChannel() {
            //TODO: Split the message if it exceeds Discord's maximum characters per message (2000).
        StringBuffer discordMessageBuffer = new StringBuffer();

        for (int windowIndex : WINDOW_ORDER) {
            CharacterMatrix charMatrix = this.windows.get(windowIndex);

            String renderedMatrix = charMatrix.render().trim();
            
            if (renderedMatrix.length() != 0) {
                    
                discordMessageBuffer.append("```");
                discordMessageBuffer.append(renderedMatrix);
                discordMessageBuffer.append("```");

                charMatrix.reset();
            }
        }

        String output = discordMessageBuffer.toString();
        if (!output.isEmpty()) {
                BotUtils.sendMessage(channel, output);
        }
    }

    private void sendToChannel(String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        pw.flush();

        StringBuffer sb = new StringBuffer();
        sb.append("```");
        sb.append(message);
        sb.append("\n\n");
        sb.append(sw.toString());
        sb.append("```");

        BotUtils.sendMessage(channel, sb.toString());
    }

    private void sendToChannel(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append("```");
        sb.append(message);
        sb.append("```");

        BotUtils.sendMessage(channel, sb.toString());
    }

    private void updateChannelTopic() {
            StringBuffer sb = new StringBuffer();
            
            if (!stopRequested && this.isRunning()) {
                sb.append("Now playing: ");
                sb.append(this.getStory().getName());
                if (statusLineInitiated) {
                    sb.append(" | ");
                    sb.append(vm.statusLineObjectName());
                    sb.append(" | ");
                    sb.append(vm.statusLineScoreOrTime());                    
                }
            } else {
                sb.append("Not playing anything.");
            }
            
            RequestBuffer.request(() -> {
            try {
                channel.changeTopic(sb.toString());
            } catch (MissingPermissionsException e) {
                    // This is fine. Just ignore it.
                    LOG.debug("Could not set channel topic, and therefore could not set status line.", e);
            } catch (DiscordException e) {
                LOG.error("Topic could not be set.", e);
            }
        });
    }
            
}
