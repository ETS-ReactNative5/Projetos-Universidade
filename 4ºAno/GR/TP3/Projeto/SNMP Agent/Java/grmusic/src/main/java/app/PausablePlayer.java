package main.java.app;

import java.io.InputStream;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.Player;

public class PausablePlayer {

    private final static int NOT_STARTED = 0;
    private final static int PLAYING = 1;
    private final static int PAUSED = 2;
    private final static int FINISHED = 3;

    // the player actually doing all the work
    private final Player player;

    // locking object used to communicate with player thread
    private final Object playerLock = new Object();

    // status variable what player thread is doing/supposed to do
    private int playerStatus = NOT_STARTED;


    public PausablePlayer(final InputStream inputStream) throws JavaLayerException {
        this.player = new Player(inputStream);
    }

    public PausablePlayer(final InputStream inputStream, final AudioDevice audioDevice) throws JavaLayerException {
        this.player = new Player(inputStream, audioDevice);
    }

    /**
     * Starts playback (resumes if paused)
     */
    public void play(SNMPAgent agent) throws JavaLayerException {
        synchronized (playerLock) {
            switch (playerStatus) {
                case NOT_STARTED:
                    final Runnable r = new Runnable() {
                        public void run() {
                            playInternal(agent);
                        }
                    };
                    final Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setPriority(Thread.MAX_PRIORITY);
                    playerStatus = PLAYING;
                    t.start();
                    break;
                case PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
    }

    public long getTime() {
        return player.getPosition();
    }

    /**
     * Pauses playback. Returns true if new state is PAUSED.
     */
    public boolean pause() {
        synchronized (playerLock) {
            if (playerStatus == PLAYING) {
                playerStatus = PAUSED;
            }
            return playerStatus == PAUSED;
        }
    }

    /**
     * Resumes playback. Returns true if the new state is PLAYING.
     */
    public boolean resume() {
        synchronized (playerLock) {
            if (playerStatus == PAUSED) {
                playerStatus = PLAYING;
                playerLock.notifyAll();
            }
            return playerStatus == PLAYING;
        }
    }

    /**
     * Stops playback. If not playing, does nothing
     */
    public void stop() {
        //System.out.println("BEFORE SYNCHRONIZED");
        synchronized (playerLock) {
            //System.out.println("STOPPING SYNCHRONIZED");
            playerStatus = FINISHED;
            playerLock.notifyAll();
            //System.out.println("AFTER NOTIFYING");
        }
        //System.out.println("AFTER SYNCHRONIZED");
    }

    private void playInternal(SNMPAgent agent) {
        while (playerStatus != FINISHED) {
            agent.updatePlayTable();
            try {
                if (!player.play(1)) {
                    //System.out.println("SETTING NEXT");
                    agent.playMusic(7, null, 0);
                    break;
                }
            } catch (final JavaLayerException e) {
                break;
            }
            // check if paused or terminated
            synchronized (playerLock) {
                while (playerStatus == PAUSED) {
                    try {
                        playerLock.wait();
                    } catch (final InterruptedException e) {
                        // terminate player
                        break;
                    }
                }
            }
        }
        close();
    }

    /**
     * Closes the player, regardless of current state.
     */
    public void close() {
        //System.out.println("CLOSING");
        synchronized (playerLock) {
            //System.out.println("CLOSING SYNCHRONIZED");
            playerStatus = FINISHED;
            playerLock.notifyAll();
        }
        try {
            //System.out.println("CLOSING PLAYER");
            player.close();
            //System.out.println("AFTER CLOSING PLAYER");
        } catch (final Exception e) {
            //System.out.println("EXCEPTING");
            // ignore, we are terminating anyway
        }
        //System.out.println("WHAT HAPPENS NOW?");
    }
    
    /**
     * Gives completion status. Returns true if complete
     */
    public boolean isComplete() {
        synchronized (playerLock) {
            return player.isComplete();
        }
    }  
    
    /**
     * Gives paused status. Returns true if paused
     */
    public boolean isPaused() {
        synchronized (playerLock) {
            return playerStatus == PAUSED;
        }
    }
}