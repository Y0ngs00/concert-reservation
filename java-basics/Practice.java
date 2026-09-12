import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Practice {
    public static void main(String[] args) {
        // 1. 좌석 10개 준비 (ArrayList 활용, 8/18)
        ArrayList<Seat> seatList = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            seatList.add(new Seat(i, "일반", false));
        }
        for (int i = 8; i <= 10; i++) {
            seatList.add(new VipSeat(i, false, true));
        }

        // 2. 예약 현황 기록용 (HashMap 활용, 8/20)
        HashMap<Integer, String> reservationLog = new HashMap<>();

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== 콘서트 좌석 예약 시스템 ===");
        System.out.println("총 " + seatList.size() + "석 (1~7번 일반석, 8~10번 VIP석)");

        // 3. 예약 2번 시도 받기
        for (int trial = 1; trial <= 2; trial++) {
            System.out.print("\n예약할 좌석 번호를 입력하세요: ");
            int inputNumber = scanner.nextInt();
            System.out.print("예약자 이름을 입력하세요: ");
            String name = scanner.next();

            try {
                // 4. 입력받은 번호로 좌석 찾기 (8/19에 배운 검색 패턴)
                Seat targetSeat = null;
                for (Seat seat : seatList) {
                    if (seat.getSeatNumber() == inputNumber) {
                        targetSeat = seat;
                        break;
                    }
                }

                if (targetSeat == null) {
                    throw new SeatNotFoundException(inputNumber + "번 좌석은 존재하지 않습니다.");
                }

                // 5. 예약 처리 (오버라이딩된 reserve가 자동으로 알맞게 실행됨, 8/15)
                targetSeat.reserve(name);
                reservationLog.put(inputNumber, name);

            } catch (AlreadyReservedException | SeatNotFoundException e) {
                System.out.println("예약 실패: " + e.getMessage());
            }
        }

        // 6. 최종 예약 현황 출력 (8/20 HashMap 활용)
        System.out.println("\n=== 최종 예약 현황 ===");
        for (Integer seatNum : reservationLog.keySet()) {
            System.out.println(seatNum + "번 좌석 - " + reservationLog.get(seatNum));
        }
    }
}