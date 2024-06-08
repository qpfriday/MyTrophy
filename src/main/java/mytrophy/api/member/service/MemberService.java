package mytrophy.api.member.service;

import mytrophy.api.game.entity.Category;
import mytrophy.api.game.repository.CategoryRepository;
import mytrophy.api.member.dto.MemberDto;
import mytrophy.api.member.dto.MemberResponseDto;
import mytrophy.api.member.entity.Member;
import mytrophy.api.member.repository.MemberRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;


import org.springframework.transaction.annotation.Transactional;


@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final CategoryRepository categoryRepository;
    public MemberService(MemberRepository memberRepository, BCryptPasswordEncoder bCryptPasswordEncoder, CategoryRepository categoryRepository) {
        this.memberRepository = memberRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.categoryRepository = categoryRepository;
    }


    public Optional<Member> findBySteamId(String id) {
        return memberRepository.findBySteamId(id);
    }

    // 중복 아이디 검증
    @Transactional
    public boolean isUsernameExists(String username) {
        return memberRepository.existsByUsername(username);
    }

    // 회원 가입
    @Transactional
    public void signupMember(MemberDto memberDto) {

        if (memberRepository.existsByUsername(memberDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + memberDto.getUsername());
        }
        // 만약 회원가입 정보가 없으면 (처음 회원가입하면)
        Member signupMember = new Member();
        signupMember.setUsername(memberDto.getUsername());
        signupMember.setPassword(bCryptPasswordEncoder.encode(memberDto.getPassword()));
        signupMember.setRole("ROLE_USER");
        signupMember.setName(memberDto.getName());
        signupMember.setNickname(memberDto.getNickname());
        signupMember.setEmail(memberDto.getEmail());
        signupMember.setSteamId(memberDto.getSteamId());
        signupMember.setLoginType("mytrophy");
        signupMember.setImagePath(memberDto.getImagePath());

        memberRepository.save(signupMember);
    }

    // 회원 조회
    public MemberResponseDto getMemberDtoById(Long id) {
        Member member = memberRepository.findById(id).orElse(null);
        if (member == null) {
            return null;
        }
        return mapMemberToDto(member);
    }

    // 회원 수 조회
    public long getMemberCount() {
        return memberRepository.count();
    }

    // 회원 리스트 조회
    public List<MemberResponseDto> findAll() {
        List<Member> members = memberRepository.findAll();
        return members.stream()
                .map(this::mapMemberToDto)
                .collect(Collectors.toList());
    }

    // 회원 수정 (토큰)
    public boolean updateMemberByUsername(String username, MemberDto memberDto) {
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            throw new IllegalArgumentException("다음 Username에 해당하는 회원을 찾을 수 없습니다: " + username);
        }

        mapDtoToMember(memberDto, member);
        member.setPassword(bCryptPasswordEncoder.encode(memberDto.getPassword()));
        memberRepository.save(member);
        return true;
    }

    // 회원 수정 (id)
    public boolean updateMemberById(Long id, MemberDto memberDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("다음 ID에 해당하는 회원을 찾을 수 없습니다: " + id));

        mapDtoToMember(memberDto, member);
        member.setPassword(bCryptPasswordEncoder.encode(memberDto.getPassword()));
        memberRepository.save(member);
        return true;
    }

    // 회원삭제 (토큰)
    @Transactional
    public boolean deleteMemberByUsername(String username) {
        if (!memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("다음 ID에 해당하는 회원을 찾을 수 없습니다: " + username);
        }
        memberRepository.deleteByUsername(username);
        return true;
    }

    // 회원 삭제 (id)
    @Transactional
    public boolean deleteMemberById(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("다음 ID에 해당하는 회원을 찾을 수 없습니다: " + id);
        }
        memberRepository.deleteById(id);
        return true;
    }

    public void linkSteamAccount(String username, String steamId) {
        System.out.println("linkSteam username : "+username);
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        member.setSteamId(steamId);
        memberRepository.save(member);
    }

    private MemberResponseDto mapMemberToDto(Member member) {
        MemberResponseDto dto = new MemberResponseDto();
        dto.setUsername(member.getUsername());
        dto.setName(member.getName());
        dto.setNickname(member.getNickname());
        dto.setEmail(member.getEmail());
        dto.setSteamId(member.getSteamId());
        dto.setLoginType(member.getLoginType());
        dto.setImagePath(member.getImagePath());
        dto.setRole(member.getRole());
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());
        return dto;
    }



    private void mapDtoToMember(MemberDto memberDto, Member member) {
        member.setUsername(memberDto.getUsername());
        member.setName(memberDto.getName());
        member.setNickname(memberDto.getNickname());
        member.setEmail(memberDto.getEmail());
        member.setSteamId(memberDto.getSteamId());
        member.setLoginType(memberDto.getLoginType());
        member.setImagePath(memberDto.getImagePath());
        if (memberDto.getCategoryIds() != null) {
            List<Category> categories = memberDto.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + categoryId)))
                    .collect(Collectors.toList());
            member.setCategories(categories);
        }
    }

    private String encodePassword(String password) {
        return bCryptPasswordEncoder.encode(password);
    }

    // 회원 username으로 조회
    public Member findMemberByUsername(String username) {
        return memberRepository.findByUsername(username);
    }
}
