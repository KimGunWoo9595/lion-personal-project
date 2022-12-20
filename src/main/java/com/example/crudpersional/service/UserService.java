package com.example.crudpersional.service;

import com.example.crudpersional.config.jwt.JwtTokenUtil;
import com.example.crudpersional.domain.dto.UserJoinRequest;
import com.example.crudpersional.domain.entity.User;
import com.example.crudpersional.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor @Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.token.secret}") // yml의 값을 가져올 수 있다.
    private String secretKey;

    private long expireTimeMs = 1000 * 60 * 60; // 1시간

    public User join(UserJoinRequest userJoinRequest) {

        List<User> userList = userRepository.findByUserName(userJoinRequest.getUserName());

        if (!userList.isEmpty()) {
            throw new RuntimeException("이미 존재하는 이름입니다");
        }

        String encodePassword = encoder.encode(userJoinRequest.getPassword());

        User user = userJoinRequest.toEntity(encodePassword);

        User savedUser = userRepository.save(user);
        log.info("저장된 회원 : {}",savedUser);

        return savedUser;
    }


    //로그인 -> (1.아이디 존재 여부 2.비밀번호 일치 여부)
    public String login(String userName,String password) {
        log.info("서비스 아이디 비밀번호 :{} / {}" , userName,password);
        //1.아이디 존재 여부 체크
        User user = userRepository.findUserByUserName(userName)
                .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다"));


        //2.비밀번호 유효성 검사
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("해당 userName의 password가 잘못됐습니다");
        }
        //두 가지 확인중 예외 안났으면 Token발행
        String token = JwtTokenUtil.generateToken(userName, secretKey, expireTimeMs);
        return token;
    }


    public User getUserByUserName(String userName) {
        return userRepository.findUserByUserName(userName)
                .orElseThrow(() -> new RuntimeException("해당회원은 존재하지않습니다"));
    }

}