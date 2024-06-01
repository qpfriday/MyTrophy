package mytrophy.api.global.config;

import lombok.extern.slf4j.Slf4j;
import mytrophy.api.member.entity.Member;
import mytrophy.api.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 동일한 로그인 아이디가 있는지 검증
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member userData = memberRepository.findByUsername(username);
        System.out.println("loadUserByUsername: " + username);
        if (username != null) {
            return new CustomUserDetails(userData);
        }
        return null;
    }
}
