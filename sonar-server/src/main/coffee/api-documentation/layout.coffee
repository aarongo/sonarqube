define [
  'backbone.marionette',
  'templates/api-documentation'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator api-documentation-navigator'
    template: Templates['api-documentation-layout']

    regions:
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'

    events:
      'change #api-documentation-show-internals': 'toggleInternals'

    toggleInternals: (event) ->
      @app.webServices.toggleInternals()

    initialize: (app) ->
      @app = app.app
      @.listenTo(@app.webServices, "change", @app.refresh)
