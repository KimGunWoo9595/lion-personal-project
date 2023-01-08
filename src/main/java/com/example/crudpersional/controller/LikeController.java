package com.example.crudpersional.controller;

import com.example.crudpersional.domain.dto.Response;
import com.example.crudpersional.domain.entity.User;
import com.example.crudpersional.exceptionManager.ErrorCode;
import com.example.crudpersional.exceptionManager.UserException;
import com.example.crudpersional.mvc.dto.LikeRequest;
import com.example.crudpersional.service.LikeService;
import com.example.crudpersional.service.PostService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Slf4j
public class LikeController {
    private final PostService postService;
    private final LikeService likeService;




    @ApiOperation(value = "해당 글 좋아요", notes = "정상적인 JWT토큰 발급 받은 사용자만 해당 글 좋아요 가능")
    @PostMapping("/{postId}/likes")
    public Response like(@PathVariable Long postId, @ApiIgnore Authentication authentication) {
        likeService.likes(postId, authentication.getName());
        return Response.success("좋아요 성공");
    }

    @ApiOperation(value = "해당 글 좋아요 갯수", notes = "해당 postId에 해당하는 글의 좋아요 count 구하는 API")
    @GetMapping("/{postId}/likes")
    public Response<String> getLikeCount(@PathVariable Long postId) {
        Integer likeCount = likeService.getLikeCount(postId);
        return Response.successToMessage(String.format("%s번 게시글의 좋아요 개수 : %d", postId, likeCount));
    }










}
