package mytrophy.api.member.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mytrophy.api.global.jwt.CustomSuccessHandler;
import mytrophy.api.global.jwt.JWTUtil;
import mytrophy.api.member.dto.MemberDto;
import mytrophy.api.member.dto.SteamOpenidLoginDto;
import mytrophy.api.member.entity.Member;
import mytrophy.api.member.security.SteamAutenticationToken;
import mytrophy.api.member.security.SteamUserPrincipal;
import mytrophy.api.member.service.SteamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mytrophy.api.member.service.MemberService;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Slf4j
@AllArgsConstructor
@RestController
public class MemberController {
    private final AuthenticationManager authenticationManager;
    private final SteamService steamService;
    private final MemberService memberService;
    private final CustomSuccessHandler successHandler;
    private final JWTUtil jwtUtil;
    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<String> signupMember(@RequestBody MemberDto memberDto) {
        memberService.signupMember(memberDto);
        return new ResponseEntity<>("회원 가입 성공", HttpStatus.CREATED);
    }

    // 회원 조회
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable("id") Long id) {
        Member member = memberService.findMemberById(id);
        if (member == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(member);
    }

    // 회원 수정
    @PatchMapping("/{id}")
    public ResponseEntity<String> updateMember(@PathVariable("id") Long id, @RequestBody MemberDto memberDto) {
        boolean isUpdated = memberService.updateMemberById(id, memberDto);
        if (isUpdated) {
            return new ResponseEntity<>("회원 수정 성공", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("회원 수정 실패", HttpStatus.NOT_FOUND);
        }
    }

    // 회원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable("id") Long id) {
        boolean isDeleted = memberService.deleteMemberById(id);
        if (isDeleted) {
            return new ResponseEntity<>("회원 삭제 성공", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("회원 삭제 실패", HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/steam/login")
    public void login(HttpServletRequest request, HttpServletResponse response, @RequestHeader("access") String token) throws IOException {

            response.sendRedirect("http://localhost:8080/steam-login");

    }



    @GetMapping("/steam/login/redirect")
    public ModelAndView loginRedirect(HttpServletRequest request, HttpServletResponse response,@RequestParam Map<String, String> allRequestParams) {
        System.out.println("loginRedirect----------------------- ");
        SteamOpenidLoginDto dto = new SteamOpenidLoginDto(
                allRequestParams.get("openid.ns"),
                allRequestParams.get("openid.op_endpoint"),
                allRequestParams.get("openid.claimed_id"),
                allRequestParams.get("openid.identity"),
                allRequestParams.get("openid.return_to"),
                allRequestParams.get("openid.response_nonce"),
                allRequestParams.get("openid.assoc_handle"),
                allRequestParams.get("openid.signed"),
                allRequestParams.get("openid.sig")
        );
        String token =  allRequestParams.get("access");
        System.out.println(token);
        String currentUsername = jwtUtil.getUsername(token);
        System.out.println("currentUsername : "+currentUsername);
        String testname  =  SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("testname : "+testname);
        if(currentUsername !="anonymousUser"){
            try {
                String steamUserId = steamService.validateLoginParameters(dto);
                memberService.linkSteamAccount(currentUsername, steamUserId);
                return new ModelAndView("redirect:/steam/login/profile");

            } catch (Exception e) {
                e.printStackTrace();
                return new ModelAndView("redirect:/steam/login/failed");
            }
        }
        else{
            try {
                String                  steamUserId = steamService.validateLoginParameters(dto);
                SteamAutenticationToken authReq     = new SteamAutenticationToken(steamUserId);
                Authentication auth        = authenticationManager.authenticate(authReq);
                SecurityContext sc          = SecurityContextHolder.getContext();
                sc.setAuthentication(auth);
//            HttpSession session = request.getSession(true);
//            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
                successHandler.onAuthenticationSuccess(request,response, auth);
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                return new ModelAndView("redirect:/steam/login/failed");
            }
        }


        //return new ModelAndView("redirect:/steam/login/profile");
    }

    @GetMapping("/steam/login/profile")
    @ResponseBody
    public ResponseEntity<Object> success() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof SteamAutenticationToken) {
            SteamAutenticationToken steamAuth = (SteamAutenticationToken) authentication;
            SteamUserPrincipal principal = steamAuth.getPrincipal();
            return ResponseEntity.ok(principal);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
    }


    @GetMapping("/steam/failed")
    public String failed() {
        return "failed";
    }


}
