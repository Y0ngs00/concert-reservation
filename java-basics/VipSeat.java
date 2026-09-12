public class VipSeat extends Seat{
    private boolean hasWelcomeDrink;

    public VipSeat(int seatNumber, boolean isReserved, boolean hasWelcomeDrink) {
        super(seatNumber, "VIP", isReserved);
        this.hasWelcomeDrink = hasWelcomeDrink;
    }

    @Override
    public void reserve(String reserverName) {
        if(!isReserved()) {
            super.reserve(reserverName);    // 부모의 원래 예약 처리 로직을 그대로 실행
            System.out.println("웰컴 음료가 제공");
        } else {
            throw new AlreadyReservedException(getSeatNumber() + "번 VIP석은 이미 예약되어 있습니다.");
        }
    }
}
