package pl.herobrineai;

public final class PlayerMemory {
    public int encounters;
    public int signsPlaced;
    public int chatCount;
    public int ignoredEvents;
    public int reactions;
    public int lookedAtHerobrine;
    public double fear;
    public String lastAction = "none";
    public long lastActionTick = -1;
    public double lastX;
    public double lastY;
    public double lastZ;
    public String lastMessage = "";

    public void recordAction(String action, long tick, double x, double y, double z) {
        lastAction = action;
        lastActionTick = tick;
        lastX = x;
        lastY = y;
        lastZ = z;
        if (!"none".equals(action)) encounters++;
    }

    public void recordReaction(boolean reacted) {
        if (reacted) {
            reactions++;
            fear = Math.min(1.0, fear + 0.05);
        } else {
            ignoredEvents++;
            fear = Math.max(0.0, fear - 0.01);
        }
    }

    public void reset() {
        encounters = signsPlaced = chatCount = ignoredEvents = reactions = lookedAtHerobrine = 0;
        fear = 0;
        lastAction = "none";
        lastActionTick = -1;
        lastMessage = "";
    }
}
