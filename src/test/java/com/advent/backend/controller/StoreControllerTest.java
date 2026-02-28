package com.advent.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advent.backend.dto.StoreDto;
import com.advent.backend.entity.Member;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.GuestBookService;
import com.advent.backend.service.StoreService;
import com.advent.backend.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
class StoreControllerTest {
    @InjectMocks private StoreController storeController;

    @Mock private StoreService storeService;
    @Mock private GuestBookService guestBookService;
    @Mock private SubscriptionService subscriptionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc =
                MockMvcBuilders.standaloneSetup(storeController)
                        .setCustomArgumentResolvers(
                                new AuthenticationPrincipalArgumentResolver(),
                                new PageableHandlerMethodArgumentResolver())
                        .build();
    }

    @Test
    @DisplayName("상점 검색 API는 닉네임/상점명/프로필/종류개수/즐겨찾기 여부를 반환한다")
    void should_ReturnStoreSummaries_When_SearchStores() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Slice<StoreDto.StoreSummaryResponse> response =
                new SliceImpl<>(
                        List.of(
                                StoreDto.StoreSummaryResponse.builder()
                                        .storeId(storeId)
                                        .nickname("사장님")
                                        .storeName("맛있는 상점")
                                        .profileImage("https://image/profile.png")
                                        .sellingItemTypeCount(4)
                                        .favorite(true)
                                        .build()),
                        PageRequest.of(0, 10),
                        false);

        given(storeService.searchStores(eq(memberId), eq("맛"), any())).willReturn(response);

        mockMvc.perform(get("/stores/search").param("keyword", "맛").with(withAuth(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.content[0].nickname").value("사장님"))
                .andExpect(jsonPath("$.content[0].storeName").value("맛있는 상점"))
                .andExpect(jsonPath("$.content[0].profileImage").value("https://image/profile.png"))
                .andExpect(jsonPath("$.content[0].sellingItemTypeCount").value(4))
                .andExpect(jsonPath("$.content[0].favorite").value(true));
    }

    @Test
    @DisplayName("즐겨찾기 목록 API는 내 즐겨찾기 상점 목록을 반환한다")
    void should_ReturnFavoriteStores_When_GetMyFavorites() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Slice<StoreDto.StoreSummaryResponse> response =
                new SliceImpl<>(
                        List.of(
                                StoreDto.StoreSummaryResponse.builder()
                                        .storeId(storeId)
                                        .nickname("고명왕")
                                        .storeName("고명천국")
                                        .profileImage("https://image/favorite.png")
                                        .sellingItemTypeCount(2)
                                        .favorite(true)
                                        .build()),
                        PageRequest.of(0, 10),
                        false);

        given(storeService.getMyFavoriteStores(eq(memberId), any())).willReturn(response);

        mockMvc.perform(get("/stores/me/favorites").with(withAuth(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeName").value("고명천국"))
                .andExpect(jsonPath("$.content[0].favorite").value(true));
    }

    @Test
    @DisplayName("상점명 변경 API는 PUT 요청도 허용한다")
    void should_UpdateStoreName_When_PutRequest() throws Exception {
        UUID memberId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", "새 상점명"));

        mockMvc.perform(
                        put("/stores/me")
                                .with(withAuth(memberId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> storeNameCaptor = ArgumentCaptor.forClass(String.class);
        then(storeService).should().updateStoreName(eq(memberId), storeNameCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(storeNameCaptor.getValue()).isEqualTo("새 상점명");
    }

    @Test
    @DisplayName("고명 구매 API는 구매 서비스를 호출한다")
    void should_PurchaseItem_When_PostPurchaseApi() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(post("/stores/items/{itemId}/purchase", itemId).with(withAuth(memberId)))
                .andExpect(status().isNoContent());

        then(storeService).should().purchaseItem(eq(memberId), eq(itemId));
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
