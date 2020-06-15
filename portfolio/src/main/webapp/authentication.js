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

function renderAuthentication(){
  fetch('/authenticate').then((response) => response.json()).then((response) => {
    debugLog(response);
    const navbarList = document.getElementById('navbar-list');
    if(response.isUserLoggedIn){
      navbarList.appendChild(createAuthenticatedStatusElement(response.logoutUrl, 'Logout'));
    } else {
      navbarList.appendChild(createAuthenticatedStatusElement(response.loginUrl, 'Login'));
    }
  });
}

function createAuthenticatedStatusElement(href, text) {
  const listElement = document.createElement('li'); 
  listElement.classList.add('nav-item');
  listElement.classList.add('active');

  const link = document.createElement('a'); 
  link.classList.add('nav-link');
  link.setAttribute('href', href);
  link.innerHTML = text;

  listElement.appendChild(link);
  return listElement;
}

function debugLog(message) {
  shouldLog = false;
  if (!shouldLog) {
    return;
  }
  console.log(message)
}