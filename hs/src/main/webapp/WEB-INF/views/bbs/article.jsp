﻿<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>spring</title>

<jsp:include page="/WEB-INF/views/layout/staticHeader.jsp"/>

<style type="text/css">
.body-container {
	max-width: 800px;
}
</style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/board2.css" type="text/css">

<c:if test="${sessionScope.member.userId==dto.userId || sessionScope.member.userId=='admin'}">
	<script type="text/javascript">
		function deleteBoard() {
		    if(confirm("게시글을 삭제 하시 겠습니까 ? ")) {
			    let query = "num=${dto.num}&${query}";
			    let url = "${pageContext.request.contextPath}/bbs/delete?" + query;
		    	location.href = url;
		    }
		}
	</script>
</c:if>

</head>
<body>

<header>
	<jsp:include page="/WEB-INF/views/layout/header.jsp"/>
</header>
	
<main>
	<div class="container">
		<div class="body-container">	
			<div class="body-title">
				<h3><i class="bi bi-app"></i> 자유 게시판 </h3>
			</div>
			
			<div class="body-main">
				
				<table class="table">
					<thead>
						<tr>
							<td colspan="2" align="center">
								${dto.subject}
							</td>
						</tr>
					</thead>
					
					<tbody>
						<tr>
							<td width="50%">
								이름 : ${dto.userName}
							</td>
							<td align="right">
								${dto.reg_date} | 조회 ${dto.hitCount}
							</td>
						</tr>
						
						<tr>
							<td colspan="2" valign="top" height="200" style="border-bottom: none;">
								${dto.content}
							</td>
						</tr>
						
						<tr>
							<td colspan="2" class="text-center p-3">
								<button type="button" class="btn btn-outline-secondary btnSendBoardLike" title="좋아요"><i class="far fa-hand-point-up" style="color: ${isUserLike?'blue':'black'}"></i>&nbsp;&nbsp;<span id="boardLikeCount">${dto.boardLikeCount}</span></button>
							</td>
						</tr>
						
						<tr>
							<td colspan="2">
								이전글 :
								<c:if test="${not empty prevDto}">
									<a href="${pageContext.request.contextPath}/bbs/article?${query}&num=${prevDto.num}">${prevDto.subject}</a>
								</c:if>
							</td>
						</tr>
						<tr>
							<td colspan="2">
								다음글 :
								<c:if test="${not empty nextDto}">
									<a href="${pageContext.request.contextPath}/bbs/article?${query}&num=${nextDto.num}">${nextDto.subject}</a>
								</c:if>
							</td>
						</tr>
					</tbody>
				</table>
				
				<table class="table table-borderless">
					<tr>
						<td width="50%">
							<c:choose>
								<c:when test="${sessionScope.member.userId==dto.userId}">
									<button type="button" class="btn btn-light" onclick="location.href='${pageContext.request.contextPath}/bbs/update?num=${dto.num}&page=${page}';">수정</button>
								</c:when>
								<c:otherwise>
									<button type="button" class="btn btn-light" disabled>수정</button>
								</c:otherwise>
							</c:choose>
					    	
							<c:choose>
					    		<c:when test="${sessionScope.member.userId==dto.userId || sessionScope.member.userId=='admin'}">
					    			<button type="button" class="btn btn-light" onclick="deleteBoard();">삭제</button>
					    		</c:when>
					    		<c:otherwise>
					    			<button type="button" class="btn btn-light" disabled>삭제</button>
					    		</c:otherwise>
					    	</c:choose>
						</td>
						<td class="text-end">
							<button type="button" class="btn btn-light" onclick="location.href='${pageContext.request.contextPath}/bbs/list?${query}';">리스트</button>
						</td>
					</tr>
				</table>
				
				<div class="reply">
					<form name="replyForm" method="post">
						<div class='form-header'>
							<span class="bold">댓글</span><span> - 타인을 비방하거나 개인정보를 유출하는 글의 게시를 삼가해 주세요.</span>
						</div>
						
						<table class="table table-borderless reply-form">
							<tr>
								<td>
									<textarea class='form-control' name="content"></textarea>
								</td>
							</tr>
							<tr>
							   <td align='right'>
							        <button type='button' class='btn btn-light btnSendReply'>댓글 등록</button>
							    </td>
							 </tr>
						</table>
					</form>
					
					<div id="listReply"></div>
				</div>
				
			</div>
		</div>
	</div>
</main>

<script type="text/javascript">
function login() {
	location.href="${pageContext.request.contextPath}/member/login";
}

function ajaxFun(url, method, formData, dataType, fn, file = false) {
	const settings = {
			type: method, 
			data: formData,
			dataType:dataType,
			success:function(data) {
				fn(data);
			},
			beforeSend: function(jqXHR) {
				jqXHR.setRequestHeader('AJAX', true);
			},
			complete: function () {
			},
			error: function(jqXHR) {
				if(jqXHR.status === 403) {
					login();
					return false;
				} else if(jqXHR.status === 400) {
					alert('요청 처리가 실패 했습니다.');
					return false;
		    	}
		    	
				console.log(jqXHR.responseText);
			}
	};
	
	if(file) {
		settings.processData = false;  // file 전송시 필수. 서버로전송할 데이터를 쿼리문자열로 변환여부
		settings.contentType = false;  // file 전송시 필수. 서버에전송할 데이터의 Content-Type. 기본:application/x-www-urlencoded
	}
	
	$.ajax(url, settings);
}

// 게시글 공감 여부
$(function() {
	$(".btnSendBoardLike").click(function() {
		const $i = $(this).find("i");
		let isNoLike = $i.css("color") === "rgb(0, 0, 0)";
		let msg = isNoLike ? "게시글에 공감하시겠습니까 ? " : "게시글 공감을 취소하시겠습니까 ? ";
		
		if(! confirm(msg)){
			return false;
		}
		
		let url = "${pageContext.request.contextPath}/bbs/insertBoardLike";
		let num = "${dto.num}";
		let query = "num=" + num + "&isNoLike=" + isNoLike;
		
		const fn = function(data) {

			let state = data.state;
			if(state === "true"){
				let color = "black";
				if(isNoLike){
					color = "blue";
				}
				$i.css("color", color);
				
				let count = data.boardLikeCount;
				$("#boardLikeCount").text(count);
			}
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});

// 리스트
$(function() {
	listPage(1);
});

function listPage(page) {
	let url = "${pageContext.request.contextPath}/bbs/listReply";
	let query = "num=${dto.num}&pageNo="+page;
	let selector = "#listReply";
	
	const fn = function(data) {
		$(selector).html(data);
	}
	
	// AJAX - Text(html)
	ajaxFun(url, "get", query, "text", fn);
}

// 댓글 등록
$(function() {
	$(".btnSendReply").click(function() {
		let num = "${dto.num}";
		const $tb = $(this).closest("table");
		let content = $tb.find("textarea").val().trim();
		
		if(! content){
			$tb.find("textarea").focus();
			return false;
		}
		content = encodeURIComponent(content);
		
		let url = "${pageContext.request.contextPath}/bbs/insertReply";
		let query = "num=" + num + "&content=" + content + "&answer=0";
		
		const fn = function (data) {
			$tb.find("textarea").val("");
			let state = data.state;
			if(state === "true"){
				listPage(1);
			}else{
				alert("댓글 등록이 실패 했습니다.");
			}
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});

// 댓글 좋아요 / 싫어요
$(function() {
	$(".reply").on("click", ".btnSendReplyLike", function() {
		let replyNum = $(this).attr("data-replyNum");
		let replyLike = $(this).attr("data-replyLike");
		const $btn = $(this);
		
		let msg = "게시글에 공감하지 않으십니까 ? ";
		if(replyLike === "1"){
			msg = "게시글에 공감하십니까 ? ";
		}
		
		if(! confirm(msg)){
			return false;
		}
		
		let url = "${pageContext.request.contextPath}/bbs/insertReplyLike";
		let query = "replyNum="+replyNum+"&replyLike="+replyLike;
		
		const fn = function(data) {
			let state = data.state;
			if(state === "true"){
				let likeCount = data.likeCount;
				let disLikeCount = data.disLikeCount;
				
				$btn.parent("td").children().eq(0).find("span").html(likeCount);
				$btn.parent("td").children().eq(1).find("span").html(disLikeCount);
			}else if(state === "liked"){
				alert("게시글 공감 여부는 한번만 가능합니다.");
			}else{
				alert("게시글 공감 여부 처리가 실패했습니다.");
			}
		}
		ajaxFun(url, "post", query, "json", fn);
	})
})

// 댓글 삭제
$(function() {
	$(".reply").on("click", ".deleteReply", function() {
		if(! confirm("댓글을 삭제 하시겠습니까 ? ")){
			return false;
		}
		
		let replyNum = $(this).attr("data-replyNum");
		let page = $(this).attr("data-pageNo");
		
		let url = "${pageContext.request.contextPath}/bbs/deleteReply";
		let query = "replyNum=" + replyNum;
		
		const fn = function(data) {
			listPage(page);
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});

// 댓글별 답글 리스트
function listReplyAnswer(answer) {
	let url = "${pageContext.request.contextPath}/bbs/listReplyAnswer";
	let query = "answer=" + answer;
	let selector = "#listReplyAnswer" + answer;
	
	const fn = function(data) {
		$(selector).html(data);
	};
	
	ajaxFun(url, "get", query, "text", fn);
}

// 댓글별 답글 개수
function countReplyAnswer(answer) {
	let url = "${pageContext.request.contextPath}/bbs/countReplyAnswer";
	let query = "answer=" + answer;
	
	const fn = function(data) {
		let count = data.count;
		let selector = "#answerCount" + answer;
		$(selector).html(conut);
	};
	
	ajaxFun(url, "post", query, "json", fn);
}

// 댓글별 답글 버튼
$(function() {
	$(".reply").on("click", ".btnReplyAnswerLayout", function() {
		const $trReplyAnswer = $(this).closest("tr").next();
		
		let isVisible = $trReplyAnswer.is(":visible");
		let replyNum = $(this).attr("data-replyNum");
		
		if(isVisible){
			$trReplyAnswer.hide();
		}else{
			$trReplyAnswer.show();
			
			// 답글 리스트
			listReplyAnswer(replyNum);
			
			// 답글 개수
			countReplyAnswer(replyNum);
		}
	});
});

// 댓글별 답글 등록
$(function () {
	$(".reply").on("click", ".btnSendReplyAnswer", function() {
		let num = "${dto.num}";
		let replyNum = $(this).attr("data-replyNum");
		const $td = $(this).closest("td");
		
		let content = $td.find("textarea").val().trim();
		if(! content) {
			$td.find("textarea").focus();
			return false;
		}
		content = encodeURIComponent(content);
		
		let url = "${pageContext.request.contextPath}/bbs/insertReply";
		let query = "num="+num+"&content="+content+"&answer="+replyNum;
		
		const fn = function(data) {
			$td.find("textarea").val("");
			
			let state = data.state;
			if(state === "true"){
				listReplyAnswer(replyNum);
				countReplyAnswer(replyNum);
			}
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});

// 댓글별 답글 삭제
$(function () {
	$(".reply").on("click", ".deleteReplyAnswer", function() {
		if(! confirm('게시글을 삭제 하시겠습니까 ? ')){
			return false;
		}
		
		let replyNum = $(this).attr("data-replyNum");
		let answer = $(this).attr("data-answer");
		
		let url = "${pageContext.request.contextPath}/bbs/deleteReply";
		let query = "replyNum=" + replyNum;
		
		const fn = function(data) {
			listReplyAnswer(answer);
			countReplyAnswer(answer);
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});



</script>

<footer>
	<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
</footer>

<jsp:include page="/WEB-INF/views/layout/staticFooter.jsp"/>
</body>
</html>