<template id="navbar">
  <nav class="navbar navbar-expand-lg fixed-top navbar-dark bg-dark">
    <div class="container-fluid">
      <a class="navbar-brand" href="#">Brief Bytes</a>
      <button class="navbar-toggler p-0 border-0" type="button" @click="toggleNavbar" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
  
      <div class="navbar-collapse offcanvas-collapse" :class="{ open: toggledNavbar }">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <a class="nav-link" :class="{ active: active === 'news' }" href="/">News</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" :class="{ active: active === 'recommendations' }" href="/recommendations">Recommendations</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" :class="{ active: active === 'settings' }" href="/settings">Settings</a>
          </li>
        </ul>
        <searchform :query="query" :navbar="true"></searchform>
      </div>
    </div>
  </nav>
</template>
<script>
app.component("navbar", {
  template: "#navbar",
  data: function() {
    return {
      toggledNavbar: false
    }
  },
  props: ["active", "query"],
  methods: {
    toggleNavbar: function() {
      this.toggledNavbar = !this.toggledNavbar;
    }
  }
}
);
</script>
<style>
@media (max-width: 991.98px) {
  .offcanvas-collapse {
    position: fixed;
    top: 56px; /* Height of navbar */
    bottom: 0;
    left: 100%;
    width: 100%;
    padding-right: 1rem;
    padding-left: 1rem;
    overflow-y: auto;
    visibility: hidden;
    background-color: #343a40;
    transition: transform .3s ease-in-out, visibility .3s ease-in-out;
  }
  .offcanvas-collapse.open {
    visibility: visible;
    transform: translateX(-100%);
  }
}

form {
  margin-block-end: 0em;
}
</style>
