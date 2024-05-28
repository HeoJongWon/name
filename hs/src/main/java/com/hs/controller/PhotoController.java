package com.hs.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.hs.annotation.Controller;
import com.hs.annotation.RequestMapping;
import com.hs.annotation.RequestMethod;
import com.hs.dao.PhotoDAO;
import com.hs.domain.PhotoDTO;
import com.hs.domain.SessionInfo;
import com.hs.servlet.ModelAndView;
import com.hs.util.FileManager;
import com.hs.util.MyMultipartFile;
import com.hs.util.MyUtil;
import com.hs.util.MyUtilBootstrap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@Controller
public class PhotoController {
	
	@RequestMapping("/photo/list")
	public ModelAndView list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ModelAndView mav = new ModelAndView("photo/list");
		
		PhotoDAO dao = new PhotoDAO();
		MyUtil util = new MyUtilBootstrap();
		
		try {
			String page = req.getParameter("page");
			int current_page = 1;
			if(page != null) {
				current_page = Integer.parseInt(page);
			}
			
			// 전체 데이터 개수
			int dataCount = dao.dataCount();
			
			// 전체 페이지수
			int size = 12;
			int total_page = util.pageCount(dataCount, size);
			if(current_page > total_page) {
				current_page = total_page;
			}
			
			// 게시글 가져오기
			int offset = (current_page - 1) * size;
			if(offset < 0) offset = 0;
			
			List<PhotoDTO> list = dao.listPhoto(offset, size);
			
			// 페이징
			String cp = req.getContextPath();
			String listUrl = cp + "/photo/list";
			String articleUrl = cp + "/photo/article?page=" + current_page;
			String paging = util.paging(current_page, total_page, listUrl);
			
			// 포워딩할 list에 전달할 속성
			mav.addObject("list", list);
			mav.addObject("dataCount", dataCount);
			mav.addObject("articleUrl", articleUrl);
			mav.addObject("page", current_page);
			mav.addObject("total_page", total_page);
			mav.addObject("paging", paging);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mav;
	}
	
	@RequestMapping(value = "/photo/write", method = RequestMethod.GET)
	public ModelAndView writeForm(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ModelAndView mav = new ModelAndView("photo/write");
		mav.addObject("mode", "write");
		return mav;
	}
	
	@RequestMapping(value = "/photo/write", method = RequestMethod.POST)
	public ModelAndView writeSubmit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PhotoDAO dao = new PhotoDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		FileManager fileManager = new FileManager();
		
		// 파일 저장 경로
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator + "photo";
		
		try {
			PhotoDTO dto = new PhotoDTO();
			
			dto.setUserId(info.getUserId()); // 로그인 아이디
			dto.setSubject(req.getParameter("subject"));
			dto.setContent(req.getParameter("content"));
			
			String filename = null;
			Part p = req.getPart("selectFile");
			MyMultipartFile multipart = fileManager.doFileUpload(p, pathname);
			if(multipart != null) {
				filename = multipart.getSaveFilename();
			}
			
			if(filename != null) {
				dto.setImageFilename(filename);
				
				dao.insertPhoto(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ModelAndView("redirect:/photo/list");
	}
	
	@RequestMapping(value = "/photo/article", method = RequestMethod.GET)
	public ModelAndView article(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PhotoDAO dao = new PhotoDAO();
		String page = req.getParameter("page");
		
		try {
			long num = Long.parseLong(req.getParameter("num"));
			
			PhotoDTO dto = dao.findById(num);
			
			if(dto == null) {
				return new ModelAndView("redirect:/photo/list?page=" + page);
			}
			
			dto.setContent(dto.getContent().replaceAll("\n", "<br>"));
			
			ModelAndView mav = new ModelAndView("photo/article");
			
			mav.addObject("dto", dto);
			mav.addObject("page", page);
			
			return mav;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ModelAndView("redirect:/photo/list?page=" + page);
	}
	
	@RequestMapping(value = "/photo/update", method = RequestMethod.GET)
	public ModelAndView updateForm(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PhotoDAO dao = new PhotoDAO();
		String page = req.getParameter("page");
		
		try {
			HttpSession session = req.getSession();
			SessionInfo info = (SessionInfo)session.getAttribute("member");
			
			long num = Long.parseLong(req.getParameter("num"));
			
			PhotoDTO dto = dao.findById(num);
			
			if(dto == null || ! dto.getUserId().equals(info.getUserId())) {
				return new ModelAndView("redirect:/photo/list?page=" + page);
			}
			
			ModelAndView mav = new ModelAndView("photo/write");
			
			mav.addObject("dto", dto);
			mav.addObject("page", page);
			mav.addObject("mode", "update");
			
			return mav;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ModelAndView("redirect:/photo/list?page=" + page);
	}
	
	@RequestMapping(value = "/photo/update", method = RequestMethod.POST)
	public ModelAndView updateSubmit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PhotoDAO dao = new PhotoDAO();
		
		HttpSession session = req.getSession();
		FileManager fileManager = new FileManager();
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator + "photo";
		
		String page = req.getParameter("page");
		
		try {
			PhotoDTO dto = new PhotoDTO();
			
			dto.setNum(Long.parseLong(req.getParameter("num")));
			dto.setSubject(req.getParameter("subject"));
			dto.setContent(req.getParameter("content"));
			
			String imageFilename = req.getParameter("imageFilename");
			dto.setImageFilename(imageFilename);
			
			Part p = req.getPart("selectFile");
			MyMultipartFile multipart = fileManager.doFileUpload(p, pathname);
			
			if(multipart != null) {
				String filename = multipart.getSaveFilename();
				dto.setImageFilename(filename);
				
				// 기존 파일 지우기
				fileManager.doFiledelete(pathname, imageFilename);
			}
			
			dao.updatePhoto(dto);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ModelAndView("redirect:/photo/list?page="+page);
	}
	
	@RequestMapping(value = "/photo/delete", method = RequestMethod.GET)
	public ModelAndView delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		return new ModelAndView("redirect:/photo/list");
	}
	
	
}
