package com.hs.filter;

import java.io.IOException;

import com.hs.domain.SessionInfo;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// 모든 요청이 들어오는 경우 요청전에 해야 할일과 요청 처리후에 해야할일을 작성  
@WebFilter("/*")
public class LoginFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// 초기화
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// request 필터
		// 요청을 처리하기 전 작업
		
		// 로그인 체크
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		String cp = req.getContextPath();
		
		HttpSession session = req.getSession();
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		if(info == null && isExcludeUri(req) == false) {
			if(isAjaxRequest(req)) {
				// AJAX 에서 로그인이 되어 있지 않으면 403이라는 에러 코드를 던짐
				resp.sendError(403);
			} else {
				// 로그인 전주소가 존재하는 경우 전주소로 가기위해 전주소 저장
				// 로그인 전주소로 가는 작업은 로그인 처리에서 작성
				String uri = req.getRequestURI();

				// uri에서 cp 제거
				if(uri.indexOf(req.getContextPath()) == 0) {
					uri = uri.substring(req.getContextPath().length());
				}
				uri = "redirect:" + uri;
				
				// query string
				String queryString = req.getQueryString();
				if(queryString != null) {
					uri += "?" + queryString;
				}
				session.setAttribute("preLoginURI", uri);
				
				resp.sendRedirect(cp + "/member/login");
			}
			
			return;
		}
		
		// 다른 필터 또는 마지막 필터인 경우 해당 자원(서블릿, jsp 등)을 실행 
		chain.doFilter(request, response);
		
		// response 필터
		// 요청을 처리하고 후 작업
	}
	
	@Override
	public void destroy() {
		// 객체가 소멸될 때
	}
	
	// 요청이 AJAX 인지를 확인하는 메소드
	protected boolean isAjaxRequest(HttpServletRequest req) {
		// AJAX 요청인 경우 AJAX라는 이름으로 true를 헤더에 실어서 보낼 예정
		String h = req.getHeader("AJAX");
		
		return h != null && h.equals("true");
	}
	
	// 로그인 체크가 필요하지 않은지의 여부 판단
	protected boolean isExcludeUri(HttpServletRequest req) {
		String uri = req.getRequestURI();
		String cp = req.getContextPath();
		
		uri = uri.substring(cp.length()); // uri 에서 cp를 제외
		
		// 로그인 체크가 필요없는 uri
		String[] uris = {
			"/index.jsp", "/main",
			"/member/login", "/member/logout",
			"/member/member", "/member/userIdCheck",
			"/notice/list",
			"/guest/main", " /guest/list",
			"/uploads/photo/**",
			"/resources/**"
		};
		
		if(uri.length() <= 1) {
			return true;
		}
		
		for(String s : uris) {
			if(s.lastIndexOf("**") != -1) {
				s = s.substring(0, s.lastIndexOf("**"));
				if(uri.indexOf(s) == 0) {
					return true;
				}
			} else if(uri.equals(s)) {
				return true;
			}
		}
		
		return false;
	}
	
	

}
