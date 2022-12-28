package com.example.crudpersional.service;

import com.example.crudpersional.config.jwt.JwtTokenUtil;
import com.example.crudpersional.domain.dto.user.*;
import com.example.crudpersional.domain.entity.User;
import com.example.crudpersional.domain.entity.UserRole;
import com.example.crudpersional.exceptionManager.ErrorCode;
import com.example.crudpersional.exceptionManager.UserException;
import com.example.crudpersional.mvc.dto.MemberForm;
import com.example.crudpersional.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor @Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.token.secret}") // yml의 값을 가져올 수 있다.
    private String secretKey;

    private long expireTimeMs = 1000 * 60 * 60; // 1시간

    public User join(UserJoinRequest userJoinRequest) {
        log.info("서비스 단 유저:{}",userJoinRequest);
        List<User> userList = userRepository.findByUserName(userJoinRequest.getUserName());

        if (!userList.isEmpty()) {
            throw new UserException(ErrorCode.DUPLICATED_USER_NAME,String.format("%s은 이미 가입된 이름 입니다.", userJoinRequest.getUserName()));
        }

        String encodePassword = encoder.encode(userJoinRequest.getPassword());

        User user = userJoinRequest.toEntity(encodePassword);

        User savedUser = userRepository.save(user);
        log.info("저장된 회원 : {}",savedUser);

        return savedUser;
    }


    //로그인 -> (1.아이디 존재 여부 2.비밀번호 일치 여부) -> 성공 시 토큰 응답
    public String login(String userName,String password) {
        log.info("서비스 아이디 비밀번호 :{} / {}" , userName,password);
        //1.아이디 존재 여부 체크
        User user = userRepository.findUserByUserName(userName)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND,String.format("%s은 등록되어있지 않은 이름 입니다.", userName)));


        //2.비밀번호 유효성 검사
        if (!encoder.matches(password, user.getPassword())) {
            throw new UserException(ErrorCode.INVALID_PASSWORD,"해당 userName의 password가 잘못됐습니다");
        }
        //두 가지 확인중 예외 안났으면 Token발행
        String token = JwtTokenUtil.generateToken(userName, secretKey, expireTimeMs);
        return token;
    }


    public User getUserByUserName(String userName) {
        return userRepository.findUserByUserName(userName)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND,String.format("%s은 등록되어있지 않은 이름 입니다.", userName)));
    }

    //회원 조회
    @Transactional(readOnly = true)
    public UserSelectResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USERNAME_NOT_FOUND, String.format("%d번의 회원을 찾을 수 없습니다.", userId)));


        UserSelectResponse userSelectResponse
                = new UserSelectResponse(user.getId(), user.getUserName(), user.getRole());

        return userSelectResponse;
    }

    //회원 전체 조회
    @Transactional(readOnly = true)
    public List<UserListResponse> getUsers() {
        List<User> users = userRepository.findAll();
        List<UserListResponse> userListResponses = users.stream().map(u -> new UserListResponse(u.getId(), u.getUserName()))
                .collect(toList());
        return userListResponses;
    }

    @Transactional
    public UserAdminResponse changeRole(String name, Long id, UserRoleDto userRoleDto) {
        log.info("name : {}",name);
        log.info("userRoleDto : {}",userRoleDto);
        //회원 검증 + UserRole 검증 메서드
        User user = checkUserRole(name, id, userRoleDto);
        UserAdminResponse userAdminResponse = UserAdminResponse.transferResponse(user);
        return userAdminResponse;
    }

    private User checkUserRole(String name, Long id, UserRoleDto userRoleDto) {
        //주의! findUser와 user 변수 혼동 No
        //findUser는 토큰을 통해 인증 된 회원 -> 로그인된 회원
        User findUser = userRepository.findOptionalByUserName(name)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND, "해당 회원은 존재하지 않습니다"));
        log.info("findUser.getRole() :{}", findUser.getRole());
        //Admin회원만 UserRole 전환 가능
        if (!findUser.getRole().equals(UserRole.ADMIN)) {
            throw new UserException(ErrorCode.INVALID_PERMISSION, "관리자(ADMIN)만 권한 변경을 할 수 있습니다.");
        }
        //@PathVariable로 들어온 id로 조회 -> role 변환 될 대상
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, String.format("%d번 회원은 존재하지 않습니다", id)));

        //RequestBody의 값(role)이 UserRole에 해당하는 값 check (RequestBody의 값이 string으로 들어와서 "" 문자열 비교)
        if (!userRoleDto.getRole().equals("ADMIN") && !userRoleDto.getRole().equals("USER")) {
            throw new UserException(ErrorCode.USER_ROLE_NOT_FOUND, "권한은 일반회원(USER),관리자(ADMIN)입니다.");
        }
        //enum타입 값
        UserRole[] roles = UserRole.values();
        //반복 돌면서 UserRole에 해당하는 값을 User엔티티의 role필드에 setter로 넣어줌
        for (UserRole role : roles) {
            if (role.name().equals(userRoleDto.getRole())) {
                log.info("바꿔야될 role 값 :{}",role);
                user.changeRole(role);
                log.info("바꿔야될 user 값 :{}",user);
            }
        }
        return user;
    }
}
