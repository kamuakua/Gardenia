package cn.gardenia.client.gui.animation;

public enum Easing {
    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    EASE_OUT_QUAD {
        @Override
        public float apply(float t) {
            return 1 - (1 - t) * (1 - t);
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float apply(float t) {
            return 1 - (float) Math.pow(1 - t, 3);
        }
    },
    EASE_OUT_QUART {
        @Override
        public float apply(float t) {
            return 1 - (float) Math.pow(1 - t, 4);
        }
    },
    EASE_OUT_EXPO {
        @Override
        public float apply(float t) {
            return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
        }
    },
    EASE_OUT_BACK {
        @Override
        public float apply(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1;
            return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }
    },
    EASE_OUT_ELASTIC {
        @Override
        public float apply(float t) {
            if (t == 0) return 0;
            if (t == 1) return 1;
            float c4 = (float) (2 * Math.PI) / 3;
            return (float) Math.pow(2, -10 * t) * (float) Math.sin((t * 10 - 0.75f) * c4) + 1;
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
        }
    };

    public abstract float apply(float t);
}
