package net.tonberry.tonbot.plugin.ifplayer;

import com.google.common.base.Preconditions;
import com.tonberry.tonbot.common.BotUtils;
import com.tonberry.tonbot.common.TonbotTechnicalFault;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zmpp.base.Memory;
import org.zmpp.zcode.*;
import org.zmpp.zcode.InputStream;
import org.zmpp.zcode.OutputStream;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.immutable.List;
import sx.blah.discord.handle.obj.IChannel;

import java.io.*;
import java.util.ArrayList;

class GameStateManager implements SwingScreenModel, OutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateManager.class);

    private StringBuffer stringBuffer = new StringBuffer();

    private final Machine vm;

    @Getter
    private final Story story;
    private final File saveFile;
    private final IChannel channel;
    private final List<CapabilityFlag> capabilities;

    private boolean selected = true;

    private final Object userInputBlocker = new Object();
    private String suppliedInput;

    private Thread vmExecThread = null;
    private boolean stopRequested = false;

    public GameStateManager(Story story, File saveFile, IChannel channel) {
        this.story = Preconditions.checkNotNull(story, "story must be non-null.");
        this.saveFile = Preconditions.checkNotNull(saveFile, "saveFile must be non-null.");
        this.channel = Preconditions.checkNotNull(channel, "channel must be non-null.");

        ArrayList<CapabilityFlag> caps = new ArrayList<>();
        this.capabilities = JavaConversions.asScalaBuffer(caps).toList();

        this.vm = new Machine();
        Memory mem = story.getMemory();
        vm.init(mem, this);
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
        }
    }

    @Override
    public void putChar(char c) {
        stringBuffer.append(c);
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
        sendBufferToChannel();

        try {
            synchronized (userInputBlocker) {
                userInputBlocker.wait();
            }

            if (suppliedInput != null) {
                //TODO: What if suppliedInput is empty?
                char character = suppliedInput.charAt(0);

                //TODO: AFAIK, the VM can't handle multibyte characters. Better check for that.
                vm.resumeWithCharInput((int) character);
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

    }

    @Override
    public int readLine() {
        // When the machine requests line input, we assume that this is an indicator that
        // we should send out whatever is in the buffer to the channel.
        sendBufferToChannel();

        // Now we need to block the thread until the user responds with something.
        // If we don't block, then the machine will exit.
        try {
            synchronized (userInputBlocker) {
                userInputBlocker.wait();
            }

            if (suppliedInput != null) {
                vm.resumeWithLineInput(suppliedInput + "\n");
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
        return null;
    }

    @Override
    public ScreenModelWindow activeWindow() {
        return null;
    }

    @Override
    public void updateStatusLine() {

    }

    @Override
    public void splitWindow(int lines) {

    }

    @Override
    public void setWindow(int windowId) {

    }

    @Override
    public void setCursorPosition(int line, int column) {

    }

    @Override
    public Tuple2<Object, Object> cursorPosition() {
        return null;
    }

    @Override
    public void connect(Machine vm) {
        throw new UnsupportedOperationException("Not to be used.");
    }

    @Override
    public void initUI() {

    }

    @Override
    public void flushInterruptOutput() {

    }

    @Override
    public void cancelInput() {

    }

    @Override
    public void bufferMode(int flag) {

    }

    @Override
    public void eraseWindow(int windowId) {

    }

    @Override
    public void eraseLine(int value) {

    }

    @Override
    public void setTextStyle(int style) {

    }

    @Override
    public int setFont(int font) {
        return 0;
    }

    @Override
    public void setColour(int foreground, int background, int window) {

    }

    @Override
    public List<CapabilityFlag> capabilities() {
        return capabilities;
    }

    public void supplyInput(String input) {
        this.suppliedInput = input;
        synchronized (userInputBlocker) {
            userInputBlocker.notify();
        }
    }

    private void sendBufferToChannel() {
        BotUtils.sendMessage(channel, "```" + stringBuffer + "```");
        stringBuffer = new StringBuffer();
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
}
