public class EconomySeat extends Seat{
    private boolean hasLimitedView;

    public EconomySeat(int seatNumber, boolean isReserved, boolean hasLimitedView) {
        super(seatNumber, "이코노미", isReserved);
        this.hasLimitedView = hasLimitedView;
    }

    public boolean isHasLimitedView() {
        return hasLimitedView;
    }

    @Override
    public void reserve(String reserverName) {
        if (!isReserved()) {
            super.reserve(reserverName);
            if (hasLimitedView) {
                System.out.println("주의 : 이 좌석은 시야가 제한될 수 있습니다.");
            }
        }
    }

}
