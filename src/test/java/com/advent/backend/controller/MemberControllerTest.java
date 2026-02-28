package com.advent.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advent.backend.entity.Member;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {
    @InjectMocks private MemberController memberController;
    @Mock private MemberService memberService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc =
                MockMvcBuilders.standaloneSetup(memberController)
                        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                        .build();
    }

    @Test
    @DisplayName("PATCH /members/me 는 닉네임 변경 API이다")
    void should_UpdateNickname_When_PatchMembersMe() throws Exception {
        UUID memberId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("nickname", "새닉네임"));

        mockMvc.perform(
                        patch("/members/me")
                                .with(withAuth(memberId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

        then(memberService).should().updateNickName(eq(memberId), eq("새닉네임"));
    }

    @Test
    @DisplayName("닉네임 중복 검사 API는 duplicated 필드를 반환한다")
    void should_ReturnDuplicatedField_When_CheckNicknameDuplicate() throws Exception {
        given(memberService.isNicknameDuplicated("외요")).willReturn(true);

        mockMvc.perform(get("/members/nickname/duplicate").param("nickname", "외요"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicated").value(true));
    }

    @Test
    @DisplayName("PATCH /members/me/profile-image 는 프로필 이미지 변경 API이다")
    void should_UpdateProfileImage_When_PatchMembersMeProfileImage() throws Exception {
        UUID memberId = UUID.randomUUID();
        String imageUrl = "https://cdn.example.com/profile.png";
        String body = objectMapper.writeValueAsString(Map.of("profileImage", imageUrl));

        mockMvc.perform(
                        patch("/members/me/profile-image")
                                .with(withAuth(memberId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

        then(memberService).should().updateProfileImage(eq(memberId), eq(imageUrl));
    }

    private Authentication authOf(UUID memberId) {
        Member member =
                Member.builder()
                        .id(memberId)
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("social-id-" + memberId)
                        .nickname("me")
                        .build();
        CustomUserDetails principal = new CustomUserDetails(member);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private RequestPostProcessor withAuth(UUID memberId) {
        return request -> {
            Authentication authentication = authOf(memberId);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication);
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return request;
        };
    }
}
