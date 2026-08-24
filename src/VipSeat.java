public class VipSeat extends Seat{
    private boolean hasWelcomeDrink;

    public VipSeat(int seatNumber, boolean isReserved, boolean hasWelcomeDrink) {
        super(seatNumber, "VIP", isReserved);
        this.hasWelcomeDrink = hasWelcomeDrink;
    }

    //getter = boolean 이니까 get이 아니라 is
    public boolean isHasWelcomeDrink() {
        return hasWelcomeDrink;
    }

    @Override
    public void reserve(String reserverName) {
        if(!isReserved()) {
            super.reserve(reserverName);    // 부모의 원래 예약 처리 로직을 그대로 실행
            System.out.println("웰컴 음료가 제공");
        }
    }
}
