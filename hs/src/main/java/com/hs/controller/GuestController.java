package com.hs.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hs.annotation.Controller;
import com.hs.annotation.RequestMapping;
import com.hs.annotation.RequestMethod;
import com.hs.annotation.ResponseBody;
import com.hs.dao.GuestDAO;
import com.hs.domain.GuestDTO;
import com.hs.domain.SessionInfo;
import com.hs.servlet.ModelAndView;
import com.hs.util.MyUtil;
import com.hs.util.MyUtilBootstrap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class GuestController {
	@RequestMapping(value = "/guest/main", method = RequestMethod.GET)
	public ModelAndView main(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		return new ModelAndView("guest/guest");
	}
	
	// ASAX-JSON
	@ResponseBody
	@RequestMapping(value = "/guest/list", method = RequestMethod.GET)
	public Map<String, Object> list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		// 방명록 리스트
		// 넘어오는 파라미터 : [페이지번호]
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		GuestDAO dao = new GuestDAO();
		MyUtil util = new MyUtilBootstrap();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		try {
			String page = req.getParameter("pageNo");
			int current_page = 1;
			if(page != null) {
				current_page = Integer.parseInt(page);
			}
			
			int dataCount = dao.dataCount();
			int size = 5;
			int total_page = util.pageCount(dataCount, size);
			
			if(current_page > total_page){
				current_page = total_page;
			}
			
			int offset = (current_page - 1)* size;
			
			List<GuestDTO>list = dao.listGuest(offset, size);
			
			for(GuestDTO dto : list) {
				dto.setContent(dto.getContent().replaceAll(">", "&gt;"));
				dto.setContent(dto.getContent().replaceAll("<", "&lt;"));
				dto.setContent(dto.getContent().replaceAll("\n", "<br>"));
				
				if(info != null &&(info.getUserId().equals("admin") || info.getUserId().equals(dto.getUserId()))){
					dto.setDeletePermit(true);
				}
			}
			
			model.put("list", list);
			model.put("pageNo", current_page);
			model.put("total_page", total_page);
			model.put("dataCount", dataCount);
			
			model.put("state", "true");
		} catch (Exception e) {
			model.put("state","false");
		}
		
		return model;
	}
	
	// AJAX-JSON
	@ResponseBody
	@RequestMapping(value = "/guest/insert", method = RequestMethod.POST)
	public Map<String, Object> writeSubmit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		// 게시글 저장
		// 넘어온 파라미터 : 글내용
		Map<String, Object> model = new HashMap<String, Object>();
		
		GuestDAO dao = new GuestDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		
		try {
			GuestDTO dto = new GuestDTO();
			dto.setContent(req.getParameter("content"));
			dto.setUserId(info.getUserId());
			
			dao.insertGuest(dto);
			
			state = "true";
		} catch (Exception e) {
			
		}
		
		model.put("state", state);
		return model;
	}
	
	// AJAX-JSON
	@ResponseBody
	@RequestMapping(value = "/guest/delete", method = RequestMethod.POST)
	public Map<String, Object> deleteGuest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		// 게시글 삭제
		// 넘어온 파라미터 : 글번호
		Map<String, Object> model = new HashMap<String, Object>();
		
		GuestDAO dao = new GuestDAO();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		String state = "false";
		
		try {
			long num = Long.parseLong(req.getParameter("num"));
			dao.deleteGuest(num, info.getUserId());
			
			state = "true";
		} catch (Exception e) {
			
		}
		
		model.put("state", state);
		return model;
	}
	
}
