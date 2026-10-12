/**
 * Label + input + error message, so every form looks the same.
 *   <Field label="Email" error={errors.email}>
 *     <input name="email" ... />
 *   </Field>
 */
export default function Field({ label, htmlFor, error, hint, required = false, children }) {
  return (
    <div className={error ? 'field field--error' : 'field'}>
      <label className="field__label" htmlFor={htmlFor}>
        {label}
        {required && <span className="field__required" aria-hidden="true"> *</span>}
      </label>
      {children}
      {hint && !error && <p className="field__hint">{hint}</p>}
      {error && <p className="field__error">{error}</p>}
    </div>
  );
}
