package com.hs.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.hs.annotation.Controller;
import com.hs.annotation.RequestMapping;
import com.hs.annotation.RequestMethod;
import com.hs.annotation.ResponseBody;
import com.hs.dao.BoardDAO;
import com.hs.domain.BoardDTO;
import com.hs.domain.ReplyDTO;
import com.hs.domain.SessionInfo;
import com.hs.servlet.ModelAndView;
import com.hs.util.MyUtil;
import com.hs.util.MyUtilBootstrap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class BoardController {

	@RequestMapping(value = "/bbs/list")
	public ModelAndView list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 게시물 리스트
		ModelAndView mav = new ModelAndView("bbs/list");

		BoardDAO dao = new BoardDAO();
		MyUtil util = new MyUtilBootstrap();
		
		try {
			String page = req.getParameter("page");
			int current_page = 1;
			if (page != null) {
				current_page = Integer.parseInt(page);
			}
			
			// 검색
			String schType = req.getParameter("schType");
			String kwd = req.getParameter("kwd");
			if (schType == null) {
				schType = "all";
				kwd = "";
			}

			// GET 방식인 경우 디코딩
			if (req.getMethod().equalsIgnoreCase("GET")) {
				kwd = URLDecoder.decode(kwd, "utf-8");
			}

			// 전체 데이터 개수
			int dataCount;
			if (kwd.length() == 0) {
				dataCount = dao.dataCount();
			} else {
				dataCount = dao.dataCount(schType, kwd);
			}
			
			// 전체 페이지 수
			int size = 10;
			int total_page = util.pageCount(dataCount, size);
			if (current_page > total_page) {
				current_page = total_page;
			}

			// 게시물 가져오기
			int offset = (current_page - 1) * size;
			if(offset < 0) offset = 0;
			
			List<BoardDTO> list = null;
			if (kwd.length() == 0) {
				list = dao.listBoard(offset, size);
			} else {
				list = dao.listBoard(offset, size, schType, kwd);
			}

			String query = "";
			if (kwd.length() != 0) {
				query = "schType=" + schType + "&kwd=" + URLEncoder.encode(kwd, "utf-8");
			}

			// 페이징 처리
			String cp = req.getContextPath();
			String listUrl = cp + "/bbs/list";
			String articleUrl = cp + "/bbs/article?page=" + current_page;
			if (query.length() != 0) {
				listUrl += "?" + query;
				articleUrl += "&" + query;
			}

			String paging = util.paging(current_page, total_page, listUrl);

			// 포워딩할 JSP에 전달할 속성
			mav.addObject("list", list);
			mav.addObject("page", current_page);
			mav.addObject("total_page", total_page);
			mav.addObject("dataCount", dataCount);
			mav.addObject("size", size);
			mav.addObject("articleUrl", articleUrl);
			mav.addObject("paging", paging);
			mav.addObject("schType", schType);
			mav.addObject("kwd", kwd);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		// JSP로 포워딩
		return mav;
	}

	@RequestMapping(value = "/bbs/write", method = RequestMethod.GET)
	public ModelAndView writeForm(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 글쓰기 폼
		ModelAndView mav = new ModelAndView("bbs/write");
		mav.addObject("mode", "write");
		return mav;
	}

	@RequestMapping(value = "/bbs/write", method = RequestMethod.POST)
	public ModelAndView writeSubmit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 글 저장
		BoardDAO dao = new BoardDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo) session.getAttribute("member");
		
		try {
			BoardDTO dto = new BoardDTO();

			// userId는 세션에 저장된 정보
			dto.setUserId(info.getUserId());

			// 파라미터
			dto.setSubject(req.getParameter("subject"));
			dto.setContent(req.getParameter("content"));

			dao.insertBoard(dto);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ModelAndView("redirect:/bbs/list");
	}

	@RequestMapping(value = "/bbs/article", method = RequestMethod.GET)
	public ModelAndView article(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 글보기
		BoardDAO dao = new BoardDAO();
		MyUtil util = new MyUtilBootstrap();
		
		String page = req.getParameter("page");
		String query = "page=" + page;

		try {
			long num = Long.parseLong(req.getParameter("num"));
			String schType = req.getParameter("schType");
			String kwd = req.getParameter("kwd");
			if (schType == null) {
				schType = "all";
				kwd = "";
			}
			kwd = URLDecoder.decode(kwd, "utf-8");

			if (kwd.length() != 0) {
				query += "&schType=" + schType + "&kwd=" + URLEncoder.encode(kwd, "UTF-8");
			}

			// 조회수 증가
			dao.updateHitCount(num);

			// 게시물 가져오기
			BoardDTO dto = dao.findById(num);
			if (dto == null) { // 게시물이 없으면 다시 리스트로
				return new ModelAndView("redirect:/bbs/list?" + query);
			}
			dto.setContent(util.htmlSymbols(dto.getContent()));

			// 이전글 다음글
			BoardDTO prevDto = dao.findByPrev(dto.getNum(), schType, kwd);
			BoardDTO nextDto = dao.findByNext(dto.getNum(), schType, kwd);
			
			// 로그인 유지의 게시글 공감 여부
			HttpSession session = req.getSession();
			SessionInfo info = (SessionInfo)session.getAttribute("member");
			boolean isUserLike = dao.isUserBoardLike(num, info.getUserId());
			
			ModelAndView mav = new ModelAndView("bbs/article");
			
			
			// JSP로 전달할 속성
			mav.addObject("dto", dto);
			mav.addObject("page", page);
			mav.addObject("query", query);
			mav.addObject("prevDto", prevDto);
			mav.addObject("nextDto", nextDto);
			mav.addObject("isUserLike", isUserLike);
			// 포워딩
			return mav;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ModelAndView("redirect:/bbs/list?" + query);
	}

	@RequestMapping(value = "/bbs/update", method = RequestMethod.GET)
	public ModelAndView updateForm(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 수정 폼
		BoardDAO dao = new BoardDAO();

		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo) session.getAttribute("member");
		
		String page = req.getParameter("page");

		try {
			long num = Long.parseLong(req.getParameter("num"));
			BoardDTO dto = dao.findById(num);

			if (dto == null) {
				return new ModelAndView("redirect:/bbs/list?page=" + page);
			}

			// 게시물을 올린 사용자가 아니면
			if (! dto.getUserId().equals(info.getUserId())) {
				return new ModelAndView("redirect:/bbs/list?page=" + page);
			}

			ModelAndView mav = new ModelAndView("bbs/write");
			
			mav.addObject("dto", dto);
			mav.addObject("page", page);
			mav.addObject("mode", "update");

			return mav;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ModelAndView("redirect:/bbs/list?page=" + page);
	}

	@RequestMapping(value = "/bbs/update", method = RequestMethod.POST)
	public ModelAndView updateSubmit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 수정 완료
		BoardDAO dao = new BoardDAO();

		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo) session.getAttribute("member");
		
		String page = req.getParameter("page");
		try {
			BoardDTO dto = new BoardDTO();
			
			dto.setNum(Long.parseLong(req.getParameter("num")));
			dto.setSubject(req.getParameter("subject"));
			dto.setContent(req.getParameter("content"));

			dto.setUserId(info.getUserId());

			dao.updateBoard(dto);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ModelAndView("redirect:/bbs/list?page=" + page);
	}

	@RequestMapping(value = "/bbs/delete", method = RequestMethod.GET)
	public ModelAndView delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 삭제
		BoardDAO dao = new BoardDAO();

		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo) session.getAttribute("member");
		
		String page = req.getParameter("page");
		String query = "page=" + page;

		try {
			long num = Long.parseLong(req.getParameter("num"));
			String schType = req.getParameter("schType");
			String kwd = req.getParameter("kwd");
			if (schType == null) {
				schType = "all";
				kwd = "";
			}
			kwd = URLDecoder.decode(kwd, "utf-8");

			if (kwd.length() != 0) {
				query += "&schType=" + schType + "&kwd=" + URLEncoder.encode(kwd, "UTF-8");
			}

			dao.deleteBoard(num, info.getUserId());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ModelAndView("redirect:/bbs/list?" + query);
	}
	
	// 게시글 공감 저장 - AJAX/JSON
	@ResponseBody
	@RequestMapping(value = "/bbs/insertBoardLike", method = RequestMethod.POST)
	public Map<String, Object> insertBoardLike(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		// 넘어온 파라미터 : 글번호, 공감/공감취소여부
		Map<String, Object> model = new HashMap<String, Object>();
		
		BoardDAO dao = new BoardDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		int boardLikeCount = 0;
		
		try {
			long num = Long.parseLong(req.getParameter("num"));
			String isNoLike = req.getParameter("isNoLike");
			
			if(isNoLike.equals("true")) {
				// 공감
				dao.insertBoardLike(num, info.getUserId());
			}else {
				// 공감 취소
				dao.deleteBoardLike(num, info.getUserId());
			}
			
			boardLikeCount = dao.countBoardLike(num);
			
			state = "true";
		} catch (Exception e) {
		}
		
		model.put("state", state);
		model.put("boardLikeCount", boardLikeCount);
		
		return model;
	}
	
	// 댓글 저장 - AJAX-JSON
	@ResponseBody
	@RequestMapping(value = "/bbs/insertReply", method = RequestMethod.POST)
	public Map<String, Object> insertReply(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		Map<String, Object> model = new HashMap<String, Object>();
		
		// 넘어온 파라미터 : 게시글번호, 댓글, 부모번호
		
		BoardDAO dao = new BoardDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		try {
			ReplyDTO dto = new ReplyDTO();
			
			long num = Long.parseLong(req.getParameter("num"));
			dto.setNum(num);
			dto.setUserId(info.getUserId());
			dto.setContent(req.getParameter("content"));
			String answer = req.getParameter("answer");
			if(answer != null) {
				dto.setAnswer(Long.parseLong(answer));
			}
			
			dao.insertReply(dto);
			
			state = "true";
		} catch (Exception e) {
			e.printStackTrace();
     	}
		
		model.put("state", state);
		
		return model;
	}
	
	// 댓글 리스트 - AJAX - Text
	@RequestMapping(value = "/bbs/listReply", method = RequestMethod.GET)
	public ModelAndView listReply(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		// 넘어온 파라미터 : 글번호 [, 페이지번호]
		BoardDAO dao = new BoardDAO();
		MyUtil util = new MyUtilBootstrap();
		try {
			long num = Long.parseLong(req.getParameter("num"));
			String pageNo = req.getParameter("pageNo");
			int current_page = 1;
			if(pageNo != null) {
				current_page = Integer.parseInt(pageNo);
			}
			
			int size = 5;
			int total_page = 0;
			int replyCount = 0;
			
			replyCount = dao.dataCountReply(num);
			total_page = util.pageCount(replyCount, size);
			if(current_page > total_page) {
				current_page = total_page;
			}
			int offset = (current_page -1) * size;
			if(offset < 0)offset = 0;
			
			List<ReplyDTO> listReply = dao.listReply(num, offset, size);
			
			for(ReplyDTO dto : listReply) {
				dto.setContent(dto.getContent().replaceAll("\n", "<br>"));
			}
			
			// 페이징 : 자바 스크립트 함수(listPage)를 호출
			String paging = util.pagingMethod(current_page, total_page, "listPage");
			
			ModelAndView mav = new ModelAndView("bbs/listReply");
			
			mav.addObject("listReply", listReply);
			mav.addObject("pageNo", current_page);
			mav.addObject("replyCount", replyCount);
			mav.addObject("total_page", total_page);
			mav.addObject("paging", paging);
			
			
			return mav;
		} catch (Exception e) {
			e.printStackTrace();
			
			resp.sendError(400);
			
			throw e;
		}
	}
	@ResponseBody
	@RequestMapping(value = "/bbs/deleteReply", method = RequestMethod.POST)
	public Map<String, Object> deleteReply(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		Map<String, Object> model = new HashMap<String, Object>();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		
		BoardDAO dao = new BoardDAO();
		try {
			long replyNum = Long.parseLong(req.getParameter("replyNum"));
			
			dao.deleteReply(replyNum, info.getUserId());
			
			state = "true";
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		
		model.put(state, state);
		
		return model;
	}
	
	// 댓글의 답글 리스트 - AJAX : Text
	@RequestMapping(value = "/bbs/listReplyAnswer", method = RequestMethod.GET)
	public ModelAndView listRelpyAnswer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		BoardDAO dao = new BoardDAO();
		
		try {
			long answer = Long.parseLong(req.getParameter("answer"));
			
			List<ReplyDTO> listReplyAnswer = dao.listReplyAnswer(answer);
			
			for(ReplyDTO dto : listReplyAnswer) {
				dto.setContent(dto.getContent().replaceAll("\n", "<br>"));
			}
			
			ModelAndView mav = new ModelAndView("bbs/listReplyAnswer");
			mav.addObject("listReplyAnswer", listReplyAnswer);
			return mav;
		} catch (Exception e) {
			e.printStackTrace();
			resp.sendError(400);
			throw e;
		}
	}
	
	// 댓글별 답글 개수 : AJAX : JOSN
	@RequestMapping(value = "/bbs/countReplyAnswer", method = RequestMethod.POST)
	public Map<String, Object> countReplyAnswer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		Map<String, Object>model = new HashMap<String, Object>();
		
		BoardDAO dao = new BoardDAO();
		int count = 0;
		try {
			long answer = Long.parseLong(req.getParameter("answer"));
			count = dao.dataCountReplyAnswer(answer);
			
			model.put("state", "true");
		} catch (Exception e) {
			model.put("state", "false");
		}
		model.put("count", count);
		
		return model;
	}
	
	// AJAX - 댓글 공감/비공감 저장 : JSON
	@ResponseBody
	@RequestMapping(value = "/bbs/insertReplyLike", method = RequestMethod.POST)
	public Map<String, Object> insertReplyLike(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		Map<String, Object> model = new HashMap<String, Object>();
		
		BoardDAO dao = new BoardDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		int likeCount = 0;
		int disLikeCount = 0;
		
		try {
			long replyNun = Long.parseLong(req.getParameter("replyNum"));
			int replyLike = Integer.parseInt(req.getParameter("replyLike"));
			
			ReplyDTO dto = new ReplyDTO();
			dto.setReplyNum(replyNun);
			dto.setUserId(info.getUserId());
			dto.setReplyLike(replyLike);
			
			dao.insertReplyLike(dto);
			
			Map<String, Integer> map = dao.countReplyLike(replyNun);
			if(map.containsKey("likeCount")) {
				likeCount = map.get("likeCount");
			}
			
			if(map.containsKey("disLikeCount")) {
				disLikeCount = map.get("disLikeCount");
			}
			
			state = "true";
		} catch (SQLException e) {
			if(e.getErrorCode() == 1) {
				state = "liked";
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		model.put("state", state);
		model.put("likeCount", likeCount);
		model.put("disLikeCount", disLikeCount);
		
		return model;
	}
}
