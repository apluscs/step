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

window.onload = () => { 
  fetch('/data').then(response => response.json()).then((comments) => {
    console.log(comments);
    const comments_list = document.getElementById('comments-container');
    comments_list.innerHTML = '';
    for(i=0; i!=comments.length; ++i){
      comments_list.appendChild(
        createCommentElement(comments[i].email,comments[i].comment));
    }
  });
}

/** Creates an <li> element containing text. */
function createCommentElement(email, comment) {
  var card = document.createElement("div"); 
  add_class(card, "card");
  console.log("created card")
  
  var card_body = document.createElement("div"); 
  add_class(card_body, "card-body");
  card.appendChild(card_body)
  console.log("created card body")

  
  var title = document.createElement("h6"); 
  title.innerHTML = "From: " + email;
  add_class(title, "card-title")
  card_body.appendChild(title);
  console.log("created card title")
  
    
  var text = document.createElement("p"); 
  text.innerHTML = comment;
  add_class(text, "card-text")
  card_body.appendChild(text);
  console.log("created card text")

  return card;
}

add_class = (element, attribute) => {
  var att = document.createAttribute("class");  
  att.value = attribute;                           
  element.setAttributeNode(att);  
}