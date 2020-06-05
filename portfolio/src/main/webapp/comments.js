// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

function loadCommentsPage(pgNumber = 1){
  const commentsPerPage = parseInt(document.getElementById('comments-per-page-select').value);
  const DOM_WAIT_TIME = 750;
  debugLog("commentsPerPage=" + commentsPerPage);
  fetch("/data" + "?comments_per_page=" + commentsPerPage+ "&pg_number=" + pgNumber).then(response => response.json()).then((response) => {
    renderComments(response.comments);
    renderPagination(response.lastPage, pgNumber);
  });
  
  // need to wait so that the pagination renders and document.getElementById("pg" + pgNumber) doesn't return null
  setTimeout(function(){
    addClass(document.getElementById("pg" + pgNumber), "active");
  }, DOM_WAIT_TIME);
  
}

function renderPagination(lastPage, currPage){
  const paginationList = document.getElementById('pagination-list');
  while (paginationList.firstChild) {
    paginationList.removeChild(paginationList.lastChild);
  }
  if(lastPage <= 6){
    // all icons can fit on the screen
    renderPaginationRange(paginationList, 1, lastPage);
    return;
  } 
  
  // Outline: [1] [..] [currPage - 1] [currPage] [currPage + 1] [..] [lastPage]
  // If currPage exactly 2 away from either end, include all from currPage to that end (ex. [1][2][3])
  if(currPage >= 3){
    renderPaginationRange(paginationList, 1, 1);
    if(currPage > 3){
      paginationList.appendChild(createPageElement(".."));
    }
  }
  renderPaginationRange(paginationList, Math.max(1, currPage - 1), Math.min(lastPage, currPage + 1));
  
  if(currPage <= lastPage - 2){
    if(currPage < lastPage - 2){
      paginationList.appendChild(createPageElement(".."));
    }
    renderPaginationRange(paginationList, lastPage, lastPage);
  }
  
}

function renderPaginationRange(paginationList, start, end){
  for(i = start; i <= end; ++i){
    paginationList.appendChild(createPageElement(i));
  }
}

function createPageElement(page){    
  const listElement = document.createElement("li"); 
  addClass(listElement, "page-item")
  listElement.setAttribute("id", "pg" + page);
  
  const button = document.createElement("button"); 
  button.innerHTML = page;
  addClass(button, "btn");
  addClass(button, "btn-default");
  addClass(button, "page-link");
  button.addEventListener("click", function(){
    debugLog("button clicked for page " + page)
    loadCommentsPage(page)
  });
  
  listElement.appendChild(button);
    
  return listElement;
}

function renderComments(comments){
  debugLog(comments);
  const commentsList = document.getElementById('comments-container');
  commentsList.innerHTML = '';
  comments.forEach((comment) => {
    commentsList.appendChild(createCommentElement(comment.email, comment.comment, comment.date));
  });
}

function deleteComments(){
  const request = new Request('/delete-data', {method: 'POST'});
  fetch(request).then(response => {
    if (response.redirected) {
        window.location.href = response.url;
    }
  });
}

function createCommentElement(email, comment, time) {
  const card = document.createElement("div"); 
  addClass(card, "card");
  
  const cardBody = document.createElement("div"); 
  addClass(cardBody, "card-body");
  card.appendChild(cardBody)
  
  const title = document.createElement("h6"); 
  title.innerHTML = "From: " + email;
  addClass(title, "card-title")
  cardBody.appendChild(title);
    
  const text = document.createElement("p"); 
  text.innerHTML = comment;
  addClass(text, "card-text")
  cardBody.appendChild(text);
  
  const timeText = document.createElement("small"); 
  timeText.innerHTML = time;
  addClass(timeText, "card-text")
  cardBody.appendChild(timeText);
  
  return card;
}

function addClass(element, className){                         
  element.classList.add(className);  
}

function debugLog(message) {
  shouldLog = false;
  if (!shouldLog) {
    return;
  }
  console.log(message)
}