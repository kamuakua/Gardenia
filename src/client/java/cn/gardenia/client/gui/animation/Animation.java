package cn.gardenia.client.gui.animation;

public class Animation {
    private float current;
    private float target;
    private float speed;
    private Easing easing;

    public Animation(float initial, float speed, Easing easing) {
        this.current = initial;
        this.target = initial;
        this.speed = speed;
        this.easing = easing;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public float getTarget() {
        return target;
    }

    public float getCurrent() {
        return current;
    }

    public void update() {
        if (Math.abs(target - current) < 0.001f) {
            current = target;
            return;
        }
        float delta = target - current;
        float factor = easing.apply(Math.min(1.0f, speed));
        current += delta * factor;
    }

    public boolean isFinished() {
        return Math.abs(target - current) < 0.001f;
    }

    public void reset(float value) {
        this.current = value;
        this.target = value;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }
}
