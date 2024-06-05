package mytrophy.api.member.security;

import lombok.RequiredArgsConstructor;
import mytrophy.api.member.entity.Member;
import mytrophy.api.member.repository.MemberRepository;
import mytrophy.api.member.service.MemberService;
import mytrophy.api.member.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class SteamAuthenticationProvider implements AuthenticationProvider {

    @Lazy
    private MemberService memberService;
    private final SteamService steamService;
    private final MemberRepository memberRepository;

    @Autowired
    public SteamAuthenticationProvider(SteamService steamService, MemberRepository memberRepository) {
        this.steamService = steamService;
        this.memberRepository = memberRepository;
    }
    @Autowired
    @Lazy
    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String steamId = ((SteamAutenticationToken) authentication).getSteamId();
        Map<String, Object> userAttributes;
        try {
            userAttributes = steamService.getUserData(steamId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // 하나의 스팀아이디로 일반/소셜 각각 가입 가능할경우
//        Member member = memberService.findMemberByUsername(steamId);
//        if (member == null) {
//            String username = (String) userAttributes.get("personaname");
//            String profile = (String) userAttributes.get("avatar");
//            member = new Member();
//            member.setName(username);
//            member.setLoginType("steam");
//            member.setSteamId(steamId);
//            member.setUsername(steamId);
//            member.setImagePath(profile);
//            member.setRole("ROLE_USER");
//            member = memberRepository.save(member);
//        }
        Optional<Member> userOptional = memberService.findBySteamId(steamId);
        Member member = userOptional.orElseGet(() -> {
            String username = (String) userAttributes.get("personaname");
            String profile = (String) userAttributes.get("avatar");
            Member newMember = new Member();
            newMember.setName(username);
            newMember.setLoginType("steam");
            newMember.setSteamId(steamId);
            newMember.setUsername(steamId);
            newMember.setImagePath(profile);
            newMember.setRole("ROLE_USER");
            return memberRepository.save(newMember);
        });
        SteamUserPrincipal steamUserPrincipal = SteamUserPrincipal.create(member, userAttributes);

        return new SteamAutenticationToken(steamId, steamUserPrincipal, steamUserPrincipal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SteamAutenticationToken.class);
    }
}
