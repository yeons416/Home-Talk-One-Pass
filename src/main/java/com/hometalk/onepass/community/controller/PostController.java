package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.PostCreateRequest;
import com.hometalk.onepass.community.dto.PostListResponse;
import com.hometalk.onepass.community.dto.PostResponse;
import com.hometalk.onepass.community.dto.PostUpdateRequest;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {
    private final PostService postService;

    // 게시글 조회
    @GetMapping("/list")
    public String postList(Model model) {
        List<Post> posts = postService.postList();
        List<PostListResponse> responseList = posts.stream().map(PostListResponse::new).collect(Collectors.toList());
        model.addAttribute("posts", responseList);
        return "/community/postList";
    }

    // 게시글 상세 페이지
    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        Post post = postService.postDetail(id);
        model.addAttribute("post", new PostResponse(post));
        return "/community/postDetail";
    }

    // 게시글 작성 폼
    @GetMapping("/write")
    public String postForm(Model model) {
        model.addAttribute("post", new PostCreateRequest());
        return "community/postForm";
    }

    // 게시글 수정 폼
    @GetMapping("/edit/{id}")
    public String postForm(@PathVariable Long id, Model model) {
        Post post = postService.postDetail(id);

        PostUpdateRequest dto = new PostUpdateRequest();
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setPinned(post.isPinned());
        model.addAttribute("post", dto);
        model.addAttribute("postId", id);
        return "community/postForm";
    }

    // 게시글 등록
    @PostMapping("/save")
    public String createPost(PostCreateRequest dto) {
        Long id = postService.postSave(dto);
        return "redirect:/community/" + id;
    }

    // 게시글 수정
    @PostMapping("/edit/{id}") // 폼 태그의 action 주소와 맞춰야 합니다.
    public String updatePost(@PathVariable Long id, PostUpdateRequest dto) {
        postService.postUpdate(id, dto);
        return "redirect:/community/" + id;
    }
}
