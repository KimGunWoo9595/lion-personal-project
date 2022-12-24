package com.example.crudpersional.service;

import com.example.crudpersional.domain.dto.post.*;
import com.example.crudpersional.domain.dto.user.UserDeleteRequest;
import com.example.crudpersional.domain.entity.Post;
import com.example.crudpersional.domain.entity.User;
import com.example.crudpersional.exceptionManager.ErrorCode;
import com.example.crudpersional.exceptionManager.PostException;
import com.example.crudpersional.exceptionManager.UserException;
import com.example.crudpersional.mvc.dto.PostForm;
import com.example.crudpersional.repository.PostRepository;
import com.example.crudpersional.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostSelectResponse getPost(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        Post post = postOptional.orElseThrow(() -> new PostException(ErrorCode.POST_NOT_FOUND,"해당 글 없습니다"));
        PostSelectResponse postSelectResponse =
                new PostSelectResponse(post.getId(), post.getTitle(),
                        post.getBody(), post.getUser().getUserName(),
                        post.getRegisteredAt(), post.getUpdatedAt());
        return postSelectResponse;
    }

   public List<PostSelectResponse> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        List<PostSelectResponse> postSelectResponseList =
                posts.stream().map(p -> new PostSelectResponse(p)).collect(Collectors.toList());

        return postSelectResponseList;
    }

    public Page<Post> getViewPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

                                                                //인증으로 들어온 userName
    public PostAddResponse addPost(PostAddRequest postAddRequest, String userName) {
        log.info("서비스 userName:{}",userName);
        //userName으로 해당 User엔티티 찾아옴
        User user = userRepository.findOptionalByUserName(userName)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND, "회원가입 후 작성해주세요"));

        Post post = postAddRequest.toEntity(user);
        //save를 할때는 JpaRepository<Article,Long>를 사용해야 하기때문에
        //articleRequestDto -> 를 Article 타입으로 바꿔줘야한다.
        Post savedPost = postRepository.save(post);
        if (savedPost.getId() == null) {
            throw new RuntimeException("해당 파일은 존재하지 않습니다");
        }
        PostAddResponse postAddResponse = new PostAddResponse("포스트 등록 완료",savedPost.getId());
        return postAddResponse;
    }

    public PostAddResponse addMvcPost(PostForm postAddRequest, String userName) {
        log.info("서비스 userName:{}",userName);
        //userName으로 해당 User엔티티 찾아옴
        User user = userRepository.findOptionalByUserName(userName)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND, "회원가입 후 작성해주세요"));

        Post post = postAddRequest.toEntity(user);
        //save를 할때는 JpaRepository<Article,Long>를 사용해야 하기때문에
        //articleRequestDto -> 를 Article 타입으로 바꿔줘야한다.
        Post savedPost = postRepository.save(post);
        if (savedPost.getId() == null) {
            throw new RuntimeException("해당 파일은 존재하지 않습니다");
        }
        PostAddResponse postAddResponse = new PostAddResponse("포스트 등록 완료",savedPost.getId());
        return postAddResponse;
    }


    public PostUpdateResponse updatePost(Long postId, PostUpdateRequest postUpdateRequest,String userName) {
        log.info("수정 요청 dto :{}", postUpdateRequest);
        Post findPost =
                postRepository.findById(postId).orElseThrow(() -> new PostException(ErrorCode.POST_NOT_FOUND,"해당 글 없습니다"));

        log.info("userName:{}",userName);

        User user = userRepository.findOptionalByUserName(userName)
                .orElseThrow(() -> new UserException(ErrorCode.USERNAME_NOT_FOUND, String.format("%s not founded", userName)));
        Long userId = user.getId();

        // 수정 권한 확인
        if (userId != findPost.getUser().getId()) {
            throw new PostException(ErrorCode.INVALID_PERMISSION, "해당 회원은 수정할 권한이 없습니다");
        }
        //변경감지 수정
        findPost.setTitle(postUpdateRequest.getTitle());
        findPost.setBody(postUpdateRequest.getBody());

        PostUpdateResponse postUpdateResponse = new PostUpdateResponse("포스트 수정 완료", findPost.getId());
        return postUpdateResponse;

    }

    public PostDeleteResponse deletePost(Long postId, String userName) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        Post post =
                optionalPost.orElseThrow(() -> new PostException(ErrorCode.POST_NOT_FOUND, "해당 글은 존재하지 않아서 삭제할 수 없습니다."));

        User user = userRepository.findOptionalByUserName(userName)
                .orElseThrow(()
                        -> new UserException(ErrorCode.USERNAME_NOT_FOUND, String.format("%s not founded", userName)));

        //글을 쓴 유저가 아닌 다른 사람이 해당 글을 지우려고 할 때 예외
        if (user.getId() != post.getUser().getId()) {
            throw new UserException(ErrorCode.INVALID_PERMISSION, "당신을 글을 지울 수 있는 권한이없습니다");
        }

        postRepository.delete(post);
        PostDeleteResponse deleteResponse = new PostDeleteResponse("포스트 삭제 완료", post.getId());
        return deleteResponse;
    }
}
