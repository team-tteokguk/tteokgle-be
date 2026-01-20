## 🤖 AI Change Summary for PR #40

## 개요
이 변경 사항은 Spring Boot 애플리케이션에 Google OAuth2 인증 기능을 추가하고, 리프레시 토큰 유효성 검사 로직을 강화하며, 관련 오류 코드 및 서비스 로직을 수정하는 내용을 포함합니다. 주요 목적은 사용자 인증을 위한 새로운 기능을 구현하고, 기존 코드의 오류 처리 및 데이터 모델을 개선하는 것입니다.

## 주요 변경 사항
1. **ErrorCode.java 수정**: 새로운 오류 코드(`REFRESHTOKEN_NOT_FOUND`, `EXPIRED_TOKEN`, `INVALID_TOKEN`)가 추가되어 토큰 유효성 관련 오류를 처리할 수 있게 되었습니다.
2. **TokenDto.java 추가**: 액세스 토큰 및 리프레시 토큰 정보를 포함하는 DTO 클래스가 새로 추가되었습니다.
3. **AuthController.java 수정**: 리프레시 토큰 재발급 로직이 개선되어, 새로운 `TokenDto.TokenResponse`를 반환하도록 변경되었습니다.
4. **JwtTokenProvider.java 추가**: JWT 관련 기능을 제공하는 클래스가 추가되었습니다.
5. **RefreshToken.java 수정**: 리프레시 토큰 모델의 필드가 변경되고, TTL(Time To Live) 설정이 수정되었습니다.
6. **RefreshTokenRepository.java 수정**: `findByAuthKey` 메서드가 추가되어, 인증 키로 리프레시 토큰을 조회할 수 있게 되었습니다.
7. **RefreshTokenService.java 수정**: `isRefreshTokenValid` 메서드가 수정되어 리프레시 토큰의 유효성을 검사하는 로직이 강화되었습니다.
8. **RedisConfig.java 수정**: Redis 설정에 `@EnableConfigurationProperties` 어노테이션이 추가되었습니다.

## 위험/영향
- **기능 추가**: Google OAuth2 인증 기능이 추가됨에 따라, 기존 시스템에 새로운 의존성이 생깁니다. 이로 인해 인증 관련 버그가 발생할 가능성이 있습니다.
- **코드 변경**: 기존 코드에 대한 변경이 있으므로, 새로운 오류 코드와 Google 사용자 세부정보 처리 로직이 예상대로 작동하는지 확인해야 합니다.
- **기능적 변경**: 기존의 단순한 유효성 검사에서 복잡한 로직으로 변경되어, 잘못된 토큰 처리 시 예외가 발생할 수 있습니다.
- **의존성 증가**: `JwtTokenProvider`에 대한 의존성이 추가되어, 해당 클래스의 변경이 `RefreshTokenService`에 영향을 미칠 수 있습니다.
- **성능**: Redis와의 상호작용이 추가되어 성능에 영향을 줄 수 있습니다.

## 테스트/검증
- **단위 테스트**: `GoogleUserDetails`, `CustomOAuth2UserService`, 및 `RefreshTokenService`의 단위 테스트를 작성하여 새로운 기능이 올바르게 작동하는지 검증해야 합니다.
- **통합 테스트**: 전체 OAuth2 인증 흐름 및 JWT 토큰의 유효성 검사, Redis와의 상호작용을 테스트하여 Google 인증이 정상적으로 작동하는지 확인해야 합니다.

## 후속 조치
- **문서화**: Google OAuth2 인증 기능 및 `JwtTokenProvider`의 기능에 대한 문서를 작성하여 개발자들이 쉽게 이해하고 사용할 수 있도록 해야 합니다.
- **모니터링**: 새로운 오류 코드와 Google 인증 관련 로그를 모니터링하여 문제 발생 시 신속하게 대응할 수 있도록 해야 합니다.
- **테스트 강화**: `RefreshTokenService`의 새로운 메서드에 대한 테스트 케이스를 작성하고, 전체 시스템에서 리프레시 토큰 관련 기능의 통합 테스트를 수행해야 합니다.
