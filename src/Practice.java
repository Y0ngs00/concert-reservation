public class Practice {
    public static void main(String[] args){
        Seat normalSeat = new Seat(1, "일반", false);
        VipSeat vipSeat = new VipSeat(2, false, true);
        EconomySeat economySeat = new EconomySeat(3, false, true);

        System.out.println("=== 예약 시작 ===");
        normalSeat.reserve("박민수", "창가 자리 요청");
        vipSeat.reserve("김지영");
        economySeat.reserve("이수현");

        System.out.println("\n=== 좌석 정보 확인 ===");
        System.out.println(normalSeat.getSeatNumber() + "번, " + normalSeat.getSeatGrade() + "석, 예약: " + normalSeat.isReserved());
        System.out.println(vipSeat.getSeatNumber() + "번, " + vipSeat.getSeatGrade() + "석, 웰컴음료: " + vipSeat.isHasWelcomeDrink());
        System.out.println(economySeat.getSeatNumber() + "번, " + economySeat.getSeatGrade() + "석, 시야제한: " + economySeat.isHasLimitedView());

    }
}
