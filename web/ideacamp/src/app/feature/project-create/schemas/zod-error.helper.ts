import {ZodError} from 'zod';

/** Maps zod validation errors to field-based error messages.*/
export type FormErrors<T extends string> = Partial<Record<T,string>>;

export function mapZodErrors<T extends string>(error: ZodError): FormErrors<T>{
  const errors: FormErrors<T> = {};

  for(const issue of error.issues){
    const key = issue.path[0] as T | undefined;

    if(!key) continue;

    if(!errors[key]) errors[key] = issue.message
  }
  return errors;
}
