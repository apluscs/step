function renderAuthentication(){
  fetch('/authenticate').then((response) => response.json()).then((response) => {
    debugLog(response);
    const navbarList = document.getElementById('navbar-list');
    if(response.isUserLoggedIn){
      navbarList.appendChild(createAuthenticatedStatusElement(response.logoutUrl, "Logout"));
    } else {
      navbarList.appendChild(createAuthenticatedStatusElement(response.loginUrl, "Login"));
    }
  });
}

function createAuthenticatedStatusElement(href, text) {
  const listElement = document.createElement("li"); 
  listElement.classList.add("nav-item");
  listElement.classList.add("active");

  const link = document.createElement("a"); 
  link.classList.add("nav-link");
  link.setAttribute("href", href);
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