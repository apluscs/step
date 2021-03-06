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

google.charts.load('current', {
  packages: ['corechart']
});
google.charts.setOnLoadCallback(renderCommentsChart);

function loadCommentsPage(pgNumber = 1) {
  renderCommentsChart();
  const commentsPerPage = parseInt(document.getElementById('comments-per-page-select').value);
  debugLog('commentsPerPage=' + commentsPerPage);
  fetch('/data' + '?comments_per_page=' + commentsPerPage + '&pg_number=' + pgNumber).then(response => response.json()).then((response) => {
    renderComments(response.comments);
    renderPagination(response.lastPage, pgNumber);
  });
}

function renderCommentsChart() {
  fetch('/visualize-comments').then(response => response.json()).then((json) => {
    const table = new google.visualization.DataTable();
    table.addColumn('string', 'Word');
    table.addColumn('number', 'Count');

    // Suffix = '_word'.
    const keySuffixLength = 5;
    json.forEach((word) => {
      table.addRow([word.key.name.substring(0, word.key.name.length - keySuffixLength), word.propertyMap.count]);
    });

    const options = {
      'title': 'Most Frequent Words in Comments',
      'width': 600,
      'height': 500,
      hAxis: {
        title: 'Count',
        minValue: 0
      },
      vAxis: {
        title: 'Word'
      }
    };

    const chart = new google.visualization.BarChart(document.getElementById('chart-container'));
    chart.draw(table, options);
  });
}

function renderPagination(lastPage, currPage) {
  const paginationList = document.getElementById('pagination-list');
  while (paginationList.firstChild) {
    paginationList.removeChild(paginationList.lastChild);
  }
  if (lastPage <= 6) {
    // All icons can fit on the screen.
    renderPaginationRange(paginationList, 1, lastPage, currPage);
    return;
  }

  // Outline: [1] [..] [currPage - 1] [currPage] [currPage + 1] [..] [lastPage]
  // If currPage exactly 2 away from either end, include all from currPage to that end (ex. [1][2][3]).
  if (currPage >= 3) {
    renderPaginationRange(paginationList, 1, 1);
    if (currPage > 3) {
      paginationList.appendChild(createPageElement('..'));
    }
  }
  renderPaginationRange(paginationList, Math.max(1, currPage - 1), Math.min(lastPage, currPage + 1), currPage);

  if (currPage <= lastPage - 2) {
    if (currPage < lastPage - 2) {
      paginationList.appendChild(createPageElement('..'));
    }
    renderPaginationRange(paginationList, lastPage, lastPage);
  }
}

// If currPage is not set, page will never match with currPage.
function renderPaginationRange(paginationList, start, end, currPage = -1) {
  for (i = start; i <= end; ++i) {
    paginationList.appendChild(createPageElement(i, currPage));
  }
}

function createPageElement(page, currPage = -1) {
  const listElement = document.createElement('li');
  addClass(listElement, 'page-item')
  if (page === currPage) {
    addClass(listElement, 'active');
  }

  const button = document.createElement('button');
  button.innerHTML = page;
  addClass(button, 'btn');
  addClass(button, 'btn-default');
  addClass(button, 'page-link');
  button.addEventListener('click', function () {
    debugLog('button clicked for page ' + page)
    loadCommentsPage(page)
  });

  listElement.appendChild(button);
  return listElement;
}

function renderComments(comments) {
  fetch('/authenticate').then((response) => response.json()).then((response) => {
    userEmail = '';
    debugLog(response.isUserLoggedIn);
    if (response.isUserLoggedIn) {
      userEmail = response.userEmail;
    }
    const commentsList = document.getElementById('comments-container');
    commentsList.innerHTML = '';
    comments.forEach((comment) => {
      commentsList.appendChild(createCommentElement(comment, userEmail));
    });
    document.getElementById('comment-template').setAttribute('hidden', 'true');
  });
}

function deleteComment(comment) {
  const request = new Request(`/delete-data?id=${comment.id}`, {
    method: 'DELETE'
  });
  fetch(request)
    .then(response => {
      if (!response.ok) {
        throw Error(`Request rejected with status ${response.status}`);
      } else if (response.redirected) {
        window.location.href = response.url;
      }
    })
    .catch((error) => {
      alert('Error when deleting comment: ' + error);
    });
}

function createCommentElement(comment, userEmail) {
  const template = document.getElementById('comment-template');
  const card = template.cloneNode(true);
  card.setAttribute('id', '');
  card.querySelector('#comment-title').innerHTML = 'From: ' + comment.email;
  card.querySelector('#comment-text').innerHTML = comment.comment;
  card.querySelector('#comment-date').innerHTML = comment.date;

  // Give the user the option to delete comments they made.
  if (comment.email === userEmail) {
    const deleteLink = card.querySelector('#comment-delete-link');
    deleteLink.removeAttribute('hidden');
    deleteLink.addEventListener('click', () => {
      deleteComment(comment);
    });
  }
  document.getElementById('comments-container').appendChild(card);

  return card;
}

function addClass(element, className) {
  element.classList.add(className);
}

function debugLog(message) {
  shouldLog = false;
  if (!shouldLog) {
    return;
  }
  console.log(message)
}
