
package com.example.crudpersional.mvc.controller;

import com.example.crudpersional.domain.dto.Response;
import com.example.crudpersional.domain.dto.comment.CommentResponse;
import com.example.crudpersional.domain.dto.post.LikeResponse;
import com.example.crudpersional.domain.dto.post.PostAddRequest;
import com.example.crudpersional.domain.dto.post.PostSelectResponse;
import com.example.crudpersional.domain.entity.Comment;
import com.example.crudpersional.domain.entity.Post;
import com.example.crudpersional.domain.entity.User;
import com.example.crudpersional.exceptionManager.ErrorCode;
import com.example.crudpersional.exceptionManager.PostException;
import com.example.crudpersional.exceptionManager.UserException;
import com.example.crudpersional.mvc.dto.*;
import com.example.crudpersional.repository.CommentRepository;
import com.example.crudpersional.repository.LikeRepository;
import com.example.crudpersional.repository.PostRepository;
import com.example.crudpersional.repository.UserRepository;
import com.example.crudpersional.service.PostService;
import com.sun.xml.bind.v2.model.core.ID;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.List;
@ApiIgnore
@Controller
@RequiredArgsConstructor
@Slf4j
@Where(clause = "deleted = false")
@SQLDelete(sql = "UPDATE Post SET deleted = true WHERE post_id = ?")
public class PostMvcController {

    private final PostService postService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;


    @GetMapping("/posts/form")
    public String goWriteForm(@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) User loginMember, @ModelAttribute PostForm postForm, HttpServletResponse response) throws Exception{

        String url = "";
        //아래의 코드는 로그인을 하지 않았다면 alert를 띄우고 로그인 안내
        if (loginMember != null) {
            //세션에 저장된 user의 정보
            String userName = loginMember.getUsername();
            postForm.setUserName(userName);
            url = "writePost";
        }else{// -> 로그인 안되어 있을 시 알림창 후 메인화면으로
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('글 작성은 로그인 후에 진행해주세요🤗'); history.go(-1);</script>");
            out.flush();
        }
        return url;
        //
    }

    //validated 적용
    @PostMapping("/posts/doForm")
    public String doWriteForm(@Validated @ModelAttribute PostForm postForm, BindingResult result, String userName,HttpServletResponse response,Model model) throws Exception {
        //postForm dto에 설정한 validation에 걸릴 시 글 쓰기 폼으로 view 이동
        if(result.hasErrors()){
            return "writePost";
        }

        String url = "";
        //if문안에 조건은 제목 또는 내용이 없을 시 경고창을 띄우고 /members/loginIndex로 리다이렉트 전송
        if (postForm.getTitle()!=null && postForm.getBody()!=null) {
            postService.addMvcPost(postForm, userName);
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('글 작성이 완료되었습니다.🤗'); window.location.href = '/posts/list';</script>");
            out.flush();
        }else{
            url = "redirect:/posts/form";
        }
        return url;
    }

    @GetMapping("/posts/list")
    public String getPostList(@PageableDefault(page = 0 ,size = 10, sort ="registeredAt",
            direction = Sort.Direction.DESC) Pageable pageable, Model model,String title) throws Exception {
        //비 로그인 사용자 시 로그인 유도
        Page<Post> posts = null;
        //title 있을 시 검색조건 페이징 처리 작동
        if (title == null) {
            posts = postService.getViewPosts(pageable);
        }else{
            posts = postService.searchByTitle(pageable, title);
        }
        //new PostSelectResponse();
        //페이지블럭 처리
        //1을 더해주는 이유는 pageable은 0부터라 1을 처리하려면 1을 더해서 시작해주어야 한다.
        int nowPage = posts.getPageable().getPageNumber() + 1;
        //-1값이 들어가는 것을 막기 위해서 max값으로 두 개의 값을 넣고 더 큰 값을 넣어주게 된다.
        int startPage =  Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage+9, posts.getTotalPages());
        log.info("list:{}",posts);
        model.addAttribute("title", title);
        model.addAttribute("posts", posts);
        model.addAttribute("nowPage",nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "post/postList";
    }


    //포스트 상세보기
   /* @GetMapping("/post/getOne/{id}")
    public String getPost(@PathVariable Long id, Model model, CommentForm commentForm,Long postId,
                          @PageableDefault(size = 10,
                                  sort = "registeredAt",
                                  direction = Sort.Direction.DESC) Pageable pageable,@SessionAttribute(name = "loginMember", required = false) User loginMember) {

        log.info("id :{}" ,postId);
        log.info("상세보기 들어온다다다다" );
        PostSelectResponse postdto = postService.getPost(id);
        PostMvcResponse post = new PostMvcResponse(postdto);
        Page<CommentResponse> comments = postService.getComments(id, pageable);
        model.addAttribute("post", post);
        model.addAttribute("postId", post.getPostId());
        model.addAttribute("comments", comments);
        model.addAttribute("member", loginMember);
        return "post/postDetail";
    }*/



    @GetMapping("/post/getOne/{id}")
    public String getPost(@PathVariable Long id, Model model, CommentForm commentForm, MessagesRequest req,
                          @PageableDefault(size = 10,
                                  sort = "registeredAt",
                                  direction = Sort.Direction.DESC) Pageable pageable, @SessionAttribute(name = "loginMember", required = false) User loginMember) {
       log.info("req:{}{}",req.getPostId(),req.getFromId());
        PostSelectResponse postdto = postService.getPost(id);
        PostMvcResponse post = new PostMvcResponse(postdto);
        Post postentity = postRepository.findById(id).get();
        List<Comment> comments = commentRepository.findAllByPost(postentity);
        Integer likeCount = likeRepository.countByPost(postentity);
        //* 댓글 관련 *//*
        if (comments != null && !comments.isEmpty()) {
            model.addAttribute("comments", comments);
        }
        model.addAttribute("post", post);
        model.addAttribute("member", loginMember);
        model.addAttribute("likeCount", likeCount);
        return "post/post";
    }
// 위의 코드 페이징 처리된 코드
//    @GetMapping("/post/getOne/{id}")
//    public String getPost(@PathVariable Long id, Model model, CommentForm commentForm, MessagesRequest req,
//                          @PageableDefault(size = 10,
//                                  sort = "registeredAt",
//                                  direction = Sort.Direction.DESC) Pageable pageable, @SessionAttribute(name = "loginMember", required = false) User loginMember) {
//        log.info("req:{}{}",req.getPostId(),req.getFromId());
//        PostSelectResponse postdto = postService.getPost(id);
//        PostMvcResponse post = new PostMvcResponse(postdto);
//        Post postentity = postRepository.findById(id).get();
//        Page<Comment> comments = commentRepository.findAllByPost(postentity,pageable);
//        //* 댓글 관련 *//*
//        if (comments != null && !comments.isEmpty()) {
//            model.addAttribute("comments", comments);
//        }
//        model.addAttribute("post", post);
//        model.addAttribute("member", loginMember);
//        return "post/postDetail";
//    }
    @GetMapping("/post/{id}/edit")
    public String updatePost(@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) User loginMember, @PathVariable Long id,Model model,HttpServletResponse response) throws Exception{
        log.info("id :{}" ,id);

        if (loginMember == null) {
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('해당 글을 작성한 회원만 수정 할 권한이 있습니다.🤗'); history.go(-1);</script>");
            out.flush();
        }

        PostSelectResponse post = postService.getPost(id);
        if (!loginMember.getUsername().equals(post.getUserName())) {
            throw new UserException(ErrorCode.INVALID_PERMISSION, "해당 글을 작성한 회원만 수정 할 권한이 있습니다");
        }

        model.addAttribute("postForm", post);
        model.addAttribute("postId", id);
        log.info("타임리프에 넘길 id:{}", id);
        return "updatePost";
    }

    // 글 수정
    @PostMapping("/post/{id}/edit")
    public String doUpdatePost(@PathVariable Long id,@Validated @ModelAttribute PostForm postForm,BindingResult result,HttpServletResponse response) throws Exception{
        //postForm dto에 설정한 validation에 걸릴 시 글 쓰기 폼으로 view 이동
        if(result.hasErrors()){
            return "updatePost";
        }

        String url = "";
        //if문안에 조건은 제목 또는 내용이 없을 시 경고창을 띄우고 /members/loginIndex로 리다이렉트 전송
        if (postForm.getTitle()!=null && postForm.getBody()!=null) {
            postService.updateMvcPost(id,postForm);
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('글 수정이 완료되었습니다.🤗');location.assign('/members/loginIndex');</script>");
            out.flush();
        }else{
            url = "redirect:/post/{id}/edit";
        }
        return url;
    }

    @PostMapping("post/{id}/delete")
    public String delete(@SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) User loginMember, @PathVariable Long id, HttpServletResponse response, HttpSession session) throws Exception {

        Post post = postRepository.findById(id).orElseThrow(() -> new PostException(ErrorCode.POST_NOT_FOUND, "해당 post는 존재하지 않습니다."));
        log.info("session id:{}",loginMember.getId());
        log.info("posted user id:{}",post.getUser().getId());
        if (loginMember.getId() != post.getUser().getId() || session.getAttribute("loginMember") == null) {
            throw new PostException(ErrorCode.INVALID_PERMISSION, "글을 작성한 본인만 글을 삭제할 수 있습니다");
        }else{
            postService.deleteMvcPost(id);
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('글 삭제가 완료되었습니다.🤗');location.assign('/posts/list');</script>");
            out.flush();

        }
        return "/";
    }


    @ApiOperation(value = "해당 글 좋아요", notes = "정상적인 JWT토큰 발급 받은 사용자만 해당 글 좋아요 가능")
    @ResponseBody
    @PostMapping("/api/v1/posts/mvc/likes")
    public Response<LikeResponse> likeMvc(@RequestBody LikeRequest request, @SessionAttribute(name = "loginMember", required = false) User loginMember, HttpServletResponse response) throws Exception {
        log.info("좋아요 버튼 클릭 후 값 :{} / {}",request.getPostId(),loginMember);
        //세션에 저장된 user의 정보
        if (loginMember == null) {
            throw new UserException(ErrorCode.USERNAME_NOT_FOUND);
        }
        LikeResponse likeResponse = postService.likes(request.getPostId(), loginMember.getUsername());
        return Response.success(likeResponse);
    }

}

