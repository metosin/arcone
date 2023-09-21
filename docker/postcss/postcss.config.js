module.exports = (ctx) => ({
  map: ctx.options.map,
  plugins: [
    require("postcss-import"),
    require("postcss-mixins"),
    require("postcss-nested"),
    require("stylelint"),
    require("postcss-preset-env")({ stage: 1 }),
    require("cssnano"),
  ],
})
