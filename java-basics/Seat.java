public class Seat {
    private int seatNumber;
    private String seatGrade;
    protected boolean isReserved;

    public Seat(int seatNumber, String seatGrade, boolean isReserved){
        this.seatNumber = seatNumber;
        this.seatGrade = seatGrade;
        this.isReserved = isReserved;
    }

    // Gatter
    public int getSeatNumber() {
        return seatNumber;
    }

    public String getSeatGrade() {
        return seatGrade;
    }

    public boolean isReserved() {   // boolean은 관례상 get 대신 is를 붙힌다.
        return isReserved;
    }

    public void reserve(String reserverName) {
        if (isReserved) {
            throw new AlreadyReservedException(seatNumber + "번 좌석은 이미 예약되어 있습니다.");
        }
        this.isReserved = true;
        System.out.println(seatNumber + "번 좌석이 " + reserverName + "님 이름으로 예약되었습니다.");
    }

}
