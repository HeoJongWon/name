<%@ page contentType="text/html; charset=UTF-8" %>
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

.guest-form textarea {
	width: 100%; height: 75px; resize: none;
}

.item-delete, .item-notify {
	cursor: pointer;
}
.item-delete:hover, .item-notify:hover {
	text-decoration: underline; color: #f28011;
}

textarea::placeholder{
	opacity: 1; /* 파이어폭스에서 뿌옇게 나오는 현상 제거 */
	color: #333;
	text-align: center;
	line-height: 60px;
}
</style>

<script type="text/javascript">
function login() {
	location.href = "${pageContext.request.contextPath}/member/login";
}

function ajaxFun(url, method, query, dataType, fn) {
	const sentinelNode = document.querySelector('.sentinel');
	
	$.ajax({
		type:method,
		url:url,
		data:query,
		dataType:dataType,
		success:function(data){
			fn(data);
		},
		beforeSend:function(jqXHR) {
			sentinelNode.setAttribute('data-loading', 'true');
			
			jqXHR.setRequestHeader("AJAX", true);
		},
		error:function(jqXHR) {
			if(jqXHR.status === 403) {
				login();
				return false;
			} else if(jqXHR.status === 400) {
				alert("요청 처리가 실패 했습니다.");
				return false;
			}
			console.log(jqXHR.responseText);
		}
	});
}
</script>

</head>
<body>

<header>
	<jsp:include page="/WEB-INF/views/layout/header.jsp"/>
</header>
	
<main>
	<div class="container">
		<div class="body-container">	
			<div class="body-title">
				<h3><i class="bi bi-pencil-square"></i> 방명록 </h3>
			</div>
			
			<div class="body-main">
				
				<form name="guestForm" method="post">
					<div class="guest-form border border-secondary mt-5 p-3">
						<div class="p-1">
							<span class="fw-bold">방명록쓰기</span><span> - 타인을 비방하거나 개인정보를 유출하는 글의 게시를 삼가해 주세요.</span>
						</div>
						<div class="p-1">
							<textarea name="content" id="content" class="form-control" placeholder="${empty sessionScope.member ? '로그인 후 등록 가능합니다.':''}"></textarea>
						</div>
						<div class="p-1 text-end">
							<button type="button" class="btnSend btn btn-dark" ${empty sessionScope.member ? "disabled":""}> 등록하기 <i class="bi bi-check2"></i> </button>
						</div>
					</div>
				</form>

				<div class="mt-4 mb-1 p-3 wrap-inner">
					<div class='py-2'>
						<span class='fw-bold text-primary item-count'>방명록 0개</span>
					</div>
					
					<div class="list-content" data-pageNo="0" data-totalPage="0"></div>
		
					<div class="sentinel" data-loading="false"></div>
				</div>

			</div>
		</div>
	</div>
</main>

<script type="text/javascript">
// 글리스트

// 화면의 마지막인지를 감시할 DIV
const sentinelNode = document.querySelector('.sentinel');
// 게시글을 출력할 DIV
const listNode = document.querySelector('.list-content');

function loadContent(page) {
	let url = "${pageContext.request.contextPath}/guest/list";
	let query = "pageNo=" + page;
	
	const fn = function(data) {
		addNewContent(data);
	};
	
	ajaxFun(url, "get", query, "json", fn);
}

function addNewContent(data) {
	const itemCount = document.querySelector('.item-count');
	
	let dataCount = data.dataCount;
	let pageNo = data.pageNo;
	let total_page = data.total_page;
	
	listNode.setAttribute('data-pageNo', pageNo);
	listNode.setAttribute('data-totalPage', total_page);
	
	itemCount.innerHTML = '방명록 ' + dataCount + '개';
	
	sentinelNode.style.display = 'none';
	
	if(parseInt(dataCount) === 0) {
		listNode.innerHTML = '';
		return;
	}
	
	let htmlText;
	for(let item of data.list) {
		let num = item.num;
		let userName = item.userName;
		let content = item.content;
		let reg_date = item.reg_date;
		let deletePermit = item.deletePermit;
		
		htmlText =  '<div class="item-content">';
		htmlText += '  <div class="row p-2 border bg-light">';
		htmlText += '      <div class="col">' + userName + '</div>';
		htmlText += '      <div class="col text-end">' + reg_date;
		if(deletePermit) {
			htmlText +=      ' | <span class="item-delete" data-num="' + num + '">삭제</span>'
		} else {
			htmlText +=      ' | <span class="item-notify" data-num="' + num + '">신고</span>'
		}
		htmlText += '      </div>';
		htmlText += '  </div>';
		htmlText += '  <div class="p-2 text-break">' + content + "</div>";
		htmlText += '</div>';
		
		listNode.insertAdjacentHTML('beforeend', htmlText);
	}
	
	if(pageNo < total_page) {
		sentinelNode.setAttribute('data-loading', 'false');
		sentinelNode.style.display = 'block';
		
		io.observe(sentinelNode); // 관찰할 대상 등록
	}
}

const ioCallback = (entries, io) => {
	entries.forEach((entry) => {
		if(entry.isIntersecting) { // 관찰자가 화면에 보이면
			// 현재 페이지가 로딩중이면 빠져 나감
			let loading = sentinelNode.getAttribute('data-loading');
			if(loading !== 'false') {
				return;
			}
			
			// 기존 관찰하던 요소는 더이상 관찰하지 않음
			io.unobserve(entry.target);
			
			let pageNo = parseInt(listNode.getAttribute('data-pageNo'));
			let total_page = parseInt(listNode.getAttribute('data-totalPage'));
			
			if(pageNo === 0 || pageNo < total_page) {
				pageNo++;
				loadContent(pageNo);
			}
		}
	});
};

const io = new IntersectionObserver(ioCallback); // 관찰자 초기화
io.observe(sentinelNode); // 관찰할 요소 등록 

// 글 등록
$(function(){
	$(".btnSend").click(function(){
		let content = $("#content").val().trim();
		
		if(! content) {
			$("#content").focus();
			return false;
		}
		
		let url = "${pageContext.request.contextPath}/guest/insert";
		let query = "content=" + encodeURIComponent(content);
		// let query = $("form[name=guestForm]").serialize();
		
		const fn = function(data) {
			$("#content").val("");
			$(".list-content").empty();
			// loadContent(1);
			
			listNode.setAttribute('data-pageNo', "0");
			sentinelNode.setAttribute('data-loading', 'false');
			sentinelNode.style.display = 'block';
			io.observe(sentinelNode);
		};
		
		ajaxFun(url, "post", query, "json", fn);
	});
});

// 글 삭제
$(function() {
	// 동적으로 생성된 요소의 이벤트 방법
	$(".list-content").on("click", ".item-delete", function() {
		let num = $(this).attr("data-num");
		
		if(! confirm('게시글을 삭제 하시겠습니까?')){
			return false;
		}
		
		let url = "${pageContext.request.contextPath}/guest/delete";
		let query = "num=" + num;
		
		const fn = function(data) {
			$(".list-content").empty();
			// loadContent(1);
			
			listNode.setAttribute('data-pageNo', "0");
			sentinelNode.setAttribute('data-loading', 'false');
			sentinelNode.style.display = 'block';
			io.observe(sentinelNode);
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