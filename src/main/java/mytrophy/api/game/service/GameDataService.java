package mytrophy.api.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mytrophy.api.game.entity.Achievement;
import mytrophy.api.game.entity.Game;
import mytrophy.api.game.entity.Screenshot;
import mytrophy.api.game.repository.AchievementRepository;
import mytrophy.api.game.repository.CategoryRepository;
import mytrophy.api.game.repository.GameRepository;
import mytrophy.api.game.repository.ScreenshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GameDataService {

    private final GameRepository gameRepository;
    private final AchievementRepository achievementRepository;
    private final CategoryRepository categoryRepository;
    private final ScreenshotRepository screenshotRepository;

    // application.properties에서 설정된 값 주입
    @Value("${steam.api-key}")
    private String steamKey;

    @Autowired
    public GameDataService(GameRepository gameRepository, AchievementRepository achievementRepository,
                           CategoryRepository categoryRepository, ScreenshotRepository screenshotRepository) {
        this.gameRepository = gameRepository;
        this.achievementRepository = achievementRepository;
        this.categoryRepository = categoryRepository;
        this.screenshotRepository = screenshotRepository;
    }

    // 스팀 게임 목록을 받아와 DB에 저장하는 메서드
    public void receiveSteamGameList() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://api.steampowered.com/ISteamApps/GetAppList/v2/";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        JsonNode rootNode = new ObjectMapper().readTree(response.getBody());
        JsonNode appsNode = rootNode.get("applist").get("apps");

        int count = 0;
        for (JsonNode appNode : appsNode) {
            int appId = appNode.get("appid").asInt();
            System.out.println(appId);
            gameDetail(appId);
            count++;
            if (count > 999) break;
        }
    }

    // 특정 게임의 상세 정보를 받아와 DB에 저장하는 메서드
    @Transactional
    public void gameDetail(int appId) throws JsonProcessingException {
        String url = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&l=korean";
        JsonNode appNode = getAppNodeFromUrl(url, String.valueOf(appId));
        // 게임이 아닐경우 다음 앱 검색
        Boolean isSuccess = appNode.get("success").asBoolean();
        if(!isSuccess){
            return;
        }
        String type = appNode.get("data").get("type").asText();
        if(!type.equals("game")){
            return;
        }

        // JSON 데이터를 가지고 게임 엔티티를 생성하고 저장
        Game game = createGameFromJson(appNode.get("data"), appId);
        game.setAchievementList(achievementRepository.saveAll(saveGameAchievement(appId)));
        game.setScreenshotList(screenshotRepository.saveAll(saveGameScreenshot(appNode.get("data").get("screenshots"))));
        gameRepository.save(game);
    }

    // 주어진 URL로부터 JSON 데이터를 받아와서 해당 앱의 노드를 반환하는 메서드
    private JsonNode getAppNodeFromUrl(String url, String strId) throws JsonProcessingException {
        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, null, String.class);
        JsonNode rootNode = new ObjectMapper().readTree(response.getBody());
        return rootNode.get(strId);
    }

    // JSON 데이터에서 게임 정보를 추출하여 게임 엔티티를 생성하는 메서드
    private Game createGameFromJson(JsonNode appNode, int appId) {
        Long id = Long.valueOf(appId);
        String name = appNode.hasNonNull("name") ? appNode.get("name").asText() : null;
        String description = appNode.hasNonNull("short_description") ? appNode.get("short_description").asText() : null;
        // 게임 개발사
        String gameDeveloper = "";
        JsonNode developersNode = appNode.get("developers");
        if (developersNode != null && developersNode.isArray() && developersNode.size() > 0) {
            for (JsonNode developerNode : developersNode) {
                gameDeveloper += developerNode.asText();
                gameDeveloper += ",";
            }
        }

        // 게임 공급사
        String gamePublisher = "";
        JsonNode publishersNode = appNode.get("publishers");
        if (publishersNode != null && publishersNode.isArray() && publishersNode.size() > 0) {
            for (JsonNode publisherNode : publishersNode) {
                gamePublisher += publisherNode.asText();
                gamePublisher += ",";
            }
        }
        String languages = appNode.hasNonNull("supported_languages") ? appNode.get("supported_languages").asText() : null;
        List<Boolean> checkList = languagePosible(languages);
        Boolean enPosible = checkList.get(0);
        Boolean koPosible = checkList.get(1);
        Boolean jpPosible = checkList.get(2);
        // 출시 날짜
        String date = appNode.hasNonNull("release_date") ? appNode.get("release_date").get("date").asText() : null;

        // 추천수
        Integer recommandation = appNode.hasNonNull("recommendations") ? appNode.get("recommendations").get("total").asInt() : null;

        // 헤어 이미지
        String headerImagePath = appNode.hasNonNull("header_image") ? appNode.get("header_image").asText() : null;

        // 게임 가격
        Boolean isFree = appNode.hasNonNull("is_free") ? appNode.get("is_free").asBoolean() : null;
        Integer price;
        if (isFree != null && isFree) {
            price = 0;
        } else {
            price = appNode.hasNonNull("price_overview") ?
                    appNode.get("price_overview").get("final").asInt() / 100 : null;
        }

        // 컴퓨터 권장 사양
        JsonNode requirementHader = appNode.hasNonNull("pc_requirements") ? appNode.get("pc_requirements") : null;
        String requirement = requirementHader.hasNonNull("minimum") ? requirementHader.get("minimum").asText() : null;



        return new Game(id,name,description,gameDeveloper,gamePublisher,requirement,price,date,recommandation,headerImagePath,koPosible,enPosible,jpPosible,null,null,null);
    }

    // 게임 업적을 받아와서 업적 리스트를 반환하는 메서드
    private List<Achievement> saveGameAchievement(int appId) throws JsonProcessingException {
        String url = "https://api.steampowered.com/ISteamUserStats/GetSchemaForGame/v2/?key=" + steamKey + "&appid=" + appId + "&l=koreana";
        JsonNode rootNode = getAppNodeFromUrl(url, String.valueOf(appId));
        JsonNode achievementsNode = rootNode != null ? rootNode.get("availableGameStats").get("achievements") : null;
        return achievementsNode != null ? parseAchievements(achievementsNode) : Collections.emptyList();
    }

    // JSON 데이터에서 업적 정보를 추출하여 업적 리스트를 반환하는 메서드
    private List<Achievement> parseAchievements(JsonNode achievementsNode) {
        List<Achievement> achievementList = new ArrayList<>();
        for (JsonNode achievementNode : achievementsNode) {
            String name = achievementNode.get("displayName").asText();
            String imagePath = achievementNode.get("icon").asText();
            achievementList.add(new Achievement(null, name, imagePath));
        }
        return achievementList;
    }

    // JSON 데이터에서 스크린샷 정보를 추출하여 스크린샷 리스트를 반환하는 메서드
    private List<Screenshot> saveGameScreenshot(JsonNode appsNode) {
        List<Screenshot> screenshotList = new ArrayList<>();
        if (appsNode == null) {
            return Collections.emptyList();
        }
        for (JsonNode appNode : appsNode) {
            String thumbnailImagePath = appNode.get("path_thumbnail").asText();
            String fullImagePath = appNode.get("path_full").asText();
            screenshotList.add(new Screenshot(null, thumbnailImagePath, fullImagePath));
        }
        return screenshotList;
    }

    public List<Boolean> languagePosible(String text) {

        List<Boolean> list = new ArrayList<>();

        if (text == null) {
            list.add(false);
            list.add(false);
            list.add(false);
            return list;
        }

        // "영어"가 포함되어 있는지 검사
        if (text.contains("영어")) {
            list.add(true);
        } else {
            list.add(false);
        }

        // "한국어"가 포함되어 있는지 검사
        if (text.contains("한국어")) {
            list.add(true);
        } else {
            list.add(false);
        }

        // "일본어"가 포함되어 있는지 검사
        if (text.contains("일본어")) {
            list.add(true);
        } else {
            list.add(false);
        }

        return list;
    }

    public void saveCategoryList() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Resource resource = new ClassPathResource("static/json/category.json");
            InputStream inputStream = resource.getInputStream();
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            System.out.println(jsonNode);
            // 이제 jsonNode를 사용하여 데이터를 처리합니다.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}