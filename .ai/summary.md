## 🤖 AI Change Summary for PR #41

## 개요
이번 변경 사항은 소셜 로그인 기능을 개선하고 Google 및 Kakao OAuth 인증 프로세스를 최적화하기 위해 새로운 `RestTemplateConfig` 클래스를 추가하고, `MyTteokRepository`, `MemberService`, `SocialLoginService`를 수정 및 추가하였습니다. 또한, 관련 테스트 케이스를 업데이트하여 새로운 리소스 생성 여부와 기능 검증을 강화하였습니다.

## 주요 변경 사항
1. **RestTemplateConfig 클래스 추가**: `RestTemplate` 빈을 생성하는 설정 클래스를 추가했습니다.
2. **MyTteokRepository 수정**: `existsByMemberId(UUID memberId)` 메서드를 추가하여 특정 회원 ID의 존재 여부를 확인할 수 있게 했습니다.
3. **MemberService 수정**:
   - `registerIFNew` 메서드에 기본 자산을 생성하는 로직을 추가했습니다.
   - `MyTteokRepository`와 `StoreRepository`를 주입받아 사용하도록 변경했습니다.
4. **SocialLoginService 클래스 추가**: Google 및 Kakao 소셜 로그인을 처리하는 서비스 클래스를 새로 추가했습니다.
5. **Google OAuth 설정 추가**: `application.yml` 파일에 Google 리디렉션 URI와 이메일 스코프가 추가되었습니다.
6. **Kakao OAuth 설정 수정**: Kakao 리디렉션 URI가 변경되었습니다.
7. **MemberServiceUnitTest 수정**: `MyTteokRepository`와 `StoreRepository`를 `@Mock`으로 추가하여 테스트 환경을 확장하고, `storeRepository.existsByMemberId(existingMember.getId())`에 대한 검증을 추가했습니다.
8. **테스트 케이스 수정**: `MemberServiceTest`와 `MemberServiceUnitTest`에서 새로운 기능을 검증하기 위해 테스트를 업데이트했습니다.

## 위험/영향
- 새로운 소셜 로그인 기능과 OAuth 설정 변경으로 인해 외부 API와의 통신이 필요해졌으며, 잘못된 설정은 인증 실패를 초래할 수 있습니다.
- 데이터베이스에 새로운 엔티티가 생성될 수 있어 기존 데이터와의 충돌 가능성에 유의해야 합니다.
- 새로운 리포지토리의 추가로 인해 테스트의 복잡성이 증가할 수 있으며, 올바른 Mock 설정이 이루어지지 않을 경우 테스트 실패 가능성이 있습니다.

## 테스트/검증
- `MemberServiceTest`와 `MemberServiceUnitTest`에서 새로운 기능에 대한 테스트가 추가 및 수정되었습니다.
- 소셜 로그인 기능과 OAuth 인증 흐름에 대한 통합 테스트가 필요하며, 외부 API의 응답을 모의(mock)하여 테스트할 수 있습니다.
- 새로운 테스트 케이스가 추가되었으므로, 모든 테스트를 실행하여 새로운 Mock 객체가 올바르게 작동하는지 확인해야 합니다.

## 후속 작업
- 소셜 로그인 기능과 OAuth 인증 흐름에 대한 문서화 작업이 필요합니다.
- 외부 API 호출에 대한 예외 처리를 강화하여 안정성을 높여야 합니다.
- 추가된 Mock 객체에 대한 테스트 케이스를 작성하여 각 리포지토리의 동작을 검증할 필요가 있으며, 테스트 커버리지를 높이기 위해 다른 시나리오에 대한 테스트도 고려해야 합니다.
