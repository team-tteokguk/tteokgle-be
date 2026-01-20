## 🤖 AI Change Summary for PR #39

### Chunk 1/1
### 개요
이 변경 사항은 Spring Boot 애플리케이션의 Gradle 빌드 설정, 오류 코드 정의, Google OAuth2 사용자 세부정보 클래스 추가, 사용자 서비스 수정 및 애플리케이션 구성 파일 업데이트를 포함합니다. 주요 목적은 Google OAuth2 인증 기능을 추가하고 관련 오류 코드를 정의하는 것입니다.

### 주요 변경 사항
1. **build.gradle 수정**: Gradle 플러그인 및 의존성 설정에서 코드 포맷팅을 위한 `spotless` 플러그인 설정이 정리되었습니다.
2. **ErrorCode.java 수정**: 새로운 오류 코드가 추가되어 토큰 유효성 관련 오류를 처리할 수 있게 되었습니다.
   - `INVALID_TOKEN`, `TOKEN_EXPIRED`, `TOKEN_MALFORMED`, `TOKEN_UNSUPPORTED`, `TOKEN_GENERATION_FAILED`
3. **GoogleUserDetails.java 추가**: Google OAuth2 사용자 정보를 처리하는 클래스를 새로 추가했습니다.
4. **CustomOAuth2UserService.java 수정**: Google OAuth2 사용자 정보를 처리하기 위해 `GoogleUserDetails` 클래스를 사용하도록 수정되었습니다.
5. **application.yml 수정**: Google OAuth2 클라이언트 설정이 추가되었습니다.
6. **test/resources/application.yml 수정**: 테스트 환경에서도 Google OAuth2 클라이언트 설정이 추가되었습니다.

### 위험/영향
- **기능 추가**: Google OAuth2 인증 기능이 추가됨에 따라, 기존 시스템에 새로운 의존성이 생깁니다. 이로 인해 인증 관련 버그가 발생할 가능성이 있습니다.
- **코드 변경**: 기존 코드에 대한 변경이 있으므로, 새로운 오류 코드와 Google 사용자 세부정보 처리 로직이 예상대로 작동하는지 확인해야 합니다.

### 테스트/검증
- **단위 테스트**: GoogleUserDetails 및 CustomOAuth2UserService의 단위 테스트를 작성하여 새로운 기능이 올바르게 작동하는지 검증해야 합니다.
- **통합 테스트**: 전체 OAuth2 인증 흐름을 테스트하여 Google 인증이 정상적으로 작동하는지 확인해야 합니다.

### 후속 조치
- **문서화**: Google OAuth2 인증 기능에 대한 문서를 작성하여 개발자들이 쉽게 이해하고 사용할 수 있도록 해야 합니다.
- **모니터링**: 새로운 오류 코드와 Google 인증 관련 로그를 모니터링하여 문제 발생 시 신속하게 대응할 수 있도록 해야 합니다.
