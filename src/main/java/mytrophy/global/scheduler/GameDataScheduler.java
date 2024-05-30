package mytrophy.global.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import mytrophy.api.game.service.GameDataService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GameDataScheduler {

    private final GameDataService gameDataService;
    private boolean isContinue = false;
    private int downSize = 20;
    private boolean downType = false; // false 면 스케줄러 true 면 메뉴얼
    private int sleepTime = 3000;
    private int test = 1;
    private static boolean serverFirstStartCheck = false;


    public GameDataScheduler(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
    }

    // 15일에 한 번 실행
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24 * 15) // 15일 간격으로 실행, ms 이기 때문에 * 1000을 해준다.
    public void autoGameDataDownFirstStep() throws JsonProcessingException, InterruptedException {

        System.out.println("~~~~~~~~~~~~~~~~~ 스케줄러 테스트  ~~~~~~~~~~~~~~~~~");

        if(!serverFirstStartCheck){
            serverFirstStartCheck = true;
            return;
        }

        // 스케줄러에서 동작하는 경우에만 스팀에서 전체 게임 목록을 DB에 저장한다.
        if(!this.downType) {
            // 스팀에서 전체 게임 목록을 DB에 저장
            gameDataService.receiveSteamGameList();
            // 스케줄러로 맨 처음 실행 할 때는 이어서 진행하지 않는다
            this.isContinue = false;
        }

        int testCount = 1;
        while (true) {
            System.out.println("~~~~~~~~~~~~~~~~~ 게임 상세정보 다운로드  ~~~~~~~~~~~~~~~~~");
            boolean downFinish = gameDataService.receiveSteamGameListByDb(downSize, isContinue);
            isContinue = true;
            if(downFinish) break;
            testCount++;
            if (testCount > 40) {
                break;
            }
            Thread.sleep(sleepTime); // 3초 대기 ( 시간은 잘 조정 해야겠음 )
        }
    }

    public void startManualDown(Boolean isContinue, int downSize) throws JsonProcessingException, InterruptedException {
        this.downSize = downSize;
        this.downType = true;
        this.isContinue = isContinue;
        serverFirstStartCheck = true;
        autoGameDataDownFirstStep();
    }
}