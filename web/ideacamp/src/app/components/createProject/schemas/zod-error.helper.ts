import {ZodError} from 'zod';

export type FormErrors<T extends string> = Partial<Record<T,string>>;

export function mapZodErrors<T extends string>(error: ZodError): FormErrors<T>{
  const errors: FormErrors<T> = {};

  for(const issues of error.issues){
    const key = issues.path[0] as T | undefined;

    if(!key) continue;

    if(!errors[key]) errors[key] = issues.message
  }
  return errors;
}
