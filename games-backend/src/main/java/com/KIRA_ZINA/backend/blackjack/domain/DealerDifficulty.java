package com.KIRA_ZINA.backend.blackjack.domain;

public enum DealerDifficulty {
    BASIC {
        @Override
        public boolean shouldHit(int handValue, int remainingCards) {
            return handValue < 17;
        }
    },
    CONSERVATIVE {
        @Override
        public boolean shouldHit(int handValue, int remainingCards) {
            return handValue <= 11 || handValue < 16;
        }
    },
    AGGRESSIVE {
        @Override
        public boolean shouldHit(int handValue, int remainingCards) {
            return handValue < 18;
        }
    };

    public abstract boolean shouldHit(int handValue, int remainingCards);
}
