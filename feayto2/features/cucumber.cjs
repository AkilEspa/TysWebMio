module.exports = {
  default: {
    paths: [
      'features/**/*.feature'
    ],
    requireModule: [
      'tsx/cjs'
    ],
    require: [
      'features/support/**/*.ts',
      'features/step-definitions/**/*.ts'
    ],
    format: [
      'progress'
    ]
  }
};
