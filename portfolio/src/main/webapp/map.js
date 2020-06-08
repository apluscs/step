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

/** Creates a map and adds it to the page. */
function createMap() {
  const USCenterX = 38.5, USCenterY = -98;
  const map = new google.maps.Map(
    document.getElementById('map'),
    {center: {lat: USCenterX, lng: USCenterY}, zoom: 3}
  );
}

function debugLog(message) {
  shouldLog = false;
  if (!shouldLog) {
    return;
  }
  console.log(message)
}