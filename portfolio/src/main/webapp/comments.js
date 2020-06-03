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

function loadComments(){
  const maxComments = parseInt(document.getElementById("max-comments-select").value);
  debugLog(maxComments);
  fetch("/data" + "?max_comments=" + maxComments).then(response => response.json()).then((comments) => {
    debugLog(comments);
    const commentsList = document.getElementById('comments-container');
    commentsList.innerHTML = '';
    comments.forEach((comment) => {
      commentsList.appendChild(createCommentElement(comment.email, comment.comment, comment.time));
    });
  });
}

/** Creates an <li> element containing text. */
function createCommentElement(email, comment, time) {
  const card = document.createElement("div"); 
  addClass(card, "card");
  debugLog("created card")
  
  const cardBody = document.createElement("div"); 
  addClass(cardBody, "card-body");
  card.appendChild(cardBody)
  debugLog("created card body")
  
  const title = document.createElement("h6"); 
  title.innerHTML = "From: " + email;
  addClass(title, "card-title")
  cardBody.appendChild(title);
  debugLog("created card title")
    
  const text = document.createElement("p"); 
  text.innerHTML = comment;
  addClass(text, "card-text")
  cardBody.appendChild(text);
  debugLog("created card text")
  
  const timeText = document.createElement("small"); 
  timeText.innerHTML = time;
  addClass(timeText, "card-text")
  cardBody.appendChild(timeText);
  debugLog("created card time")
  
  return card;
}

function addClass(element, attribute){
  const att = document.createAttribute("class");  
  att.value = attribute;                           
  element.setAttributeNode(att);  
}

function debugLog(message) {
  shouldLog = false;
  if (!shouldLog) {
    return;
  }
  console.log(message)
}