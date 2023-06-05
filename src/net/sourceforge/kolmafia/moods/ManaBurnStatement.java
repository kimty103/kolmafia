package net.sourceforge.kolmafia.moods;

public abstract class ManaBurnStatement {
  public abstract boolean beforeManaBurn(Object o);
  public abstract void afterManaBurn(Object o);

  public void run(Object o) {
    if (!beforeManaBurn(o)) return;

    boolean was = MoodManager.isExecuting;
    MoodManager.isExecuting = true;

    afterManaBurn(o);
    MoodManager.isExecuting = was;
  }
}
