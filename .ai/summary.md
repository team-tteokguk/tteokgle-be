## 🤖 AI Change Summary for PR #35

### 개요
이번 변경 사항은 `NotificationRepository`, `NotificationService`, 및 `GuestBookService`의 알림 처리 및 방명록 삭제 로직을 개선하고, 권한 검사를 강화하는 내용을 포함합니다. 이를 통해 코드의 가독성과 유지보수성을 높이며, 방명록 접근 권한을 명확히 하였습니다.

### 주요 변경 사항
1. **NotificationRepository 수정**:
   - `updateNotification` 메소드를 `markAllAsReadByMemberId`로 변경하여 특정 회원의 모든 읽지 않은 알림을 일괄적으로 읽음 처리하도록 수정.

2. **NotificationService 수정**:
   - `readNotification` 메소드의 매개변수를 `notificationId`에서 `memberId`로 변경하고, 해당 회원의 모든 알림을 읽음 처리하는 로직으로 수정.

3. **GuestBookService 수정**:
   - 방명록 생성 및 수정 메소드의 반환 타입을 `void`에서 `GuestBookDto.GuestBookResponse`로 변경하여 생성된 방명록 정보를 반환하도록 수정.
   - 방명록 삭제 시 저자 확인 로직을 강화하고, `validateDeletePermission` 메소드를 추가하여 작성자와 상점 주인만 삭제할 수 있도록 권한 로직을 강화.
   - `guestBookRepository.findById` 호출을 `guestBookRepository.findByIdWithMember`로 변경하여 저자 정보와 함께 게스트북을 조회하도록 수정.

4. **ErrorCode 수정**:
   - 방명록 접근 권한 관련 에러 코드를 추가하고, 비저자에 의한 삭제 시 발생하는 예외의 에러 코드를 `ErrorCode.NOT_GUESTBOOK_WRITER`에서 `ErrorCode.GUESTBOOK_ACCESS_DENIED`로 변경.

5. **테스트 수정**:
   - 변경된 메소드 호출에 맞춰 `NotificationRepositoryTest`, `NotificationServiceTest`, `GuestBookServiceTest`의 테스트를 수정 및 추가.

### 위험/영향
- **기능적 변화**: 기존의 개별 알림 읽음 처리에서 특정 회원의 모든 읽지 않은 알림을 일괄적으로 처리하는 방식으로 변경됨에 따라 기존 로직에 의존하는 부분에서 문제가 발생할 수 있음. 저자 확인 로직 강화로 비정상적인 삭제 요청에 대한 예외 처리가 명확해져 시스템 안정성이 향상됨.
- **호환성**: 기존의 `readNotification` 및 `findById` 메소드를 호출하는 코드가 새로운 메소드 시그니처와 호환되지 않으므로, 해당 코드의 수정이 필요함.

### 테스트/검증
- 변경된 메소드에 대한 단위 테스트가 추가 및 수정되어 새로운 로직이 올바르게 작동하는지 검증됨. 비저자에 의한 삭제 시 예외가 올바르게 발생하는지 확인하는 테스트가 추가되었으며, 모든 테스트가 성공적으로 통과하여 기능이 정상적으로 작동함을 확인함.

### 후속 조치
- 기존의 `readNotification` 및 `findById` 메소드를 호출하는 모든 코드의 수정이 필요함.
- 변경 사항에 대한 문서화 및 코드 주석 업데이트가 필요함.
- 추가적인 통합 테스트를 통해 전체 시스템에서의 동작을 검증할 필요가 있음.
